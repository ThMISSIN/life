package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.life.constant.PointsTypeConstant;
import com.life.context.BaseContext;
import com.life.entity.LifePointsRecord;
import com.life.entity.LifeUser;
import com.life.exception.ValidationException;
import com.life.mapper.LifePointsRecordMapper;
import com.life.mapper.UserMapper;
import com.life.query.QueryDTO;
import com.life.service.PointsService;
import com.life.service.UserService;
import com.life.vo.PointsRankVO;
import com.life.vo.PointsSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class PointsServiceImpl implements PointsService {

    @Autowired
    private UserService userService;
    @Autowired
    private LifePointsRecordMapper pointsRecordMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 积分榜前三名奖励(逗号分隔, 名次1/2/3), 结算时按位取用
     */
    @Value("${life.points.rank-rewards:100,50,20}")
    private String rankRewards;

    @Override
    public PointsSummaryVO querySummary() {
        // 1. 余额以账户字段(life_user.points)为准, 不用流水求和当余额——
        //    流水是对账留痕, 两者口径不同(如签到积分不入账的已知缺陷期, 总览会如实暴露差异);
        //    冻结积分上线后(roadmap 2.8): total = 可用积分, 冻结额另加字段展示
        Long userId = BaseContext.getCurrentId();
        LifeUser user = userService.getById(userId);
        if (user == null) {
            throw new ValidationException("用户不存在");
        }

        // 2. 单SQL聚合本月流水(月初 = 当月1日00:00, create_time >= 月初 即本月):
        //    正数和 = monthEarned, 负数和的绝对值 = monthSpent; 无流水时 ifnull 已兜底为 0
        PointsSummaryVO vo = pointsRecordMapper.sumPointsSince(userId,
                LocalDate.now().withDayOfMonth(1).atStartOfDay());

        // 3. 回填余额(聚合SQL只算月度两项, total 以账户字段为准)
        vo.setTotal(user.getPoints() == null ? 0 : user.getPoints());
        return vo;
    }

    @Override
    public List<PointsRankVO> queryRankBoard(QueryDTO query) {
        // 榜单口径: 本月正数流水(发放)聚合倒序, 查询即最新, 自然月切换自动重榜(见接口注释);
        // 分页入参兜底: pageNum/pageSize 非法时按 1/10
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        List<PointsRankVO> board = pointsRecordMapper.selectMonthRankBoard(start, end, offset, pageSize);
        // rank 回填: offset + 行序(SQL 排序 pts desc + user_id asc 稳定, 并列积分先达者在前)
        for (int i = 0; i < board.size(); i++) {
            board.get(i).setRank(offset + i + 1);
        }
        return board;
    }

    @Override
    public PointsRankVO queryMyRank() {
        Long userId = BaseContext.getCurrentId();
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        // 名次: 无正数流水 SQL 返回 0(未上榜); 本月获得: 复用 sumPointsSince 口径
        Integer rank = pointsRecordMapper.selectMyMonthRank(userId, start, end);
        PointsSummaryVO month = pointsRecordMapper.sumPointsSince(userId, start);
        LifeUser user = userService.getById(userId);
        if (user == null) {
            throw new ValidationException("用户不存在");
        }
        return PointsRankVO.builder()
                .rank(rank == null ? 0 : rank)
                .userId(userId)
                .nickname(user.getNickname() != null && !user.getNickname().isBlank()
                        ? user.getNickname() : user.getUsername())
                .username(user.getUsername())
                .avatar(user.getAvatar())
                .points(month == null || month.getMonthEarned() == null ? 0 : month.getMonthEarned())
                .build();
    }

    /**
     * 月度榜单结算(定时任务入口, 无登录态): 结算上个自然月前三名, 发放奖励积分。
     * <p>
     * 幂等: 结算批次号 order_no = "RANK:yyyyMM:名次", 发放前查重, 重复触发(任务重跑/手动补发)安全;
     * 入账与流水同事务(单用户维度整体成功/失败由外层事务统一保证, 前三名量级小);
     * 发分入口待 roadmap 2.2 changePoints 落地后收进统一链路
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleMonthlyRank() {
        // 1. 上个自然月起止 + 结算批次
        LocalDate lastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDateTime start = lastMonth.atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        String batch = "RANK:" + lastMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));

        // 2. 上月前三(口径与 C 端榜单一致: 正数流水聚合倒序; rank=行序+1)
        List<PointsRankVO> top3 = pointsRecordMapper.selectMonthRankBoard(start, end, 0, 3);
        if (top3.isEmpty()) {
            log.info("积分榜结算 {}: 空榜跳过", batch);
            return;
        }

        // 3. 逐名发放(奖励金额 yml 配置, 名次超出配置长度即止)
        String[] rewards = rankRewards.split(",");
        for (int i = 0; i < top3.size(); i++) {
            if (i >= rewards.length) {
                break;
            }
            int reward = Integer.parseInt(rewards[i].trim());
            if (reward <= 0) {
                continue;
            }
            PointsRankVO winner = top3.get(i);
            String orderNo = batch + ":" + (i + 1);
            Long settled = pointsRecordMapper.selectCount(new LambdaQueryWrapper<LifePointsRecord>()
                    .eq(LifePointsRecord::getType, PointsTypeConstant.RANK_REWARD)
                    .eq(LifePointsRecord::getOrderNo, orderNo));
            if (settled != null && settled > 0) {
                continue;
            }
            userMapper.addPoints(winner.getUserId(), reward);
            pointsRecordMapper.insert(LifePointsRecord.builder()
                    .userId(winner.getUserId())
                    .type(PointsTypeConstant.RANK_REWARD)
                    .points(reward)
                    .orderNo(orderNo)
                    .remark(lastMonth.getMonthValue() + "月积分榜第" + (i + 1) + "名奖励")
                    .build());
            log.info("积分榜结算 {}: 第{}名 userId={} 发放 {} 分", batch, i + 1, winner.getUserId(), reward);
        }
    }
}

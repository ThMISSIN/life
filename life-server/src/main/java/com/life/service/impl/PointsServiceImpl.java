package com.life.service.impl;

import com.life.context.BaseContext;
import com.life.entity.LifeUser;
import com.life.exception.ValidationException;
import com.life.mapper.LifePointsRecordMapper;
import com.life.query.QueryDTO;
import com.life.service.PointsService;
import com.life.service.UserService;
import com.life.vo.PointsRankVO;
import com.life.vo.PointsSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PointsServiceImpl implements PointsService {

    @Autowired
    private UserService userService;
    @Autowired
    private LifePointsRecordMapper pointsRecordMapper;

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

    @Override
    public void settleMonthlyRank() {
        // TODO 1. 计算上个自然月起止(如 2026-10-01 00:00 ~ 2026-11-01 00:00)与结算批次 "RANK:yyyyMM"
        // TODO 2. 聚合上月前三: selectMonthRankBoard(上月start, 上月end, 0, 3)
        // TODO 3. 逐名发放奖励(建议金额 yml 配置 @Value 注入, 避免写死):
        //         幂等 — 发放前查流水 type=RANK_REWARD 且 order_no="RANK:yyyyMM:名次" 是否已存在, 已存在跳过;
        //         落库 — life_user.points 加奖励值 + 写流水(type=RANK_REWARD, 正数, order_no=幂等键, remark="x月积分榜第n名"),
        //                余额更新与流水插入需同一事务;
        //         发分入口待 roadmap 2.2 changePoints 落地后收进统一链路
        // TODO 4. 上月无人获得积分(空榜)时直接跳过
    }
}

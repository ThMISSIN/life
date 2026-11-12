package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.life.constant.MessageConstant;
import com.life.constant.PointsRecordStatusConstant;
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
import org.springframework.transaction.support.TransactionTemplate;

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
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 积分榜前三名奖励(逗号分隔, 名次1/2/3), 结算时按位取用
     */
    @Value("${life.points.rank-rewards:100,50,20}")
    private String rankRewards;

    /**
     * 发放积分有效期(天, 2.8): 0=永久有效(默认); >0 时正向发放流水写 expire_time,
     * 由 PointsExpireTask 到期回收(回收额=min(发放额,当前余额), 台账见 reclaimExpiredPoints)
     */
    @Value("${life.points.expire-days:0}")
    private int expireDays;

    /**
     * 消费力度返积分配置(2026-11-12): enabled 开关 / points-per-yuan 每元基础返分 /
     * tiers "门槛元:加赠分,..." 达档累加(消费力度越大加赠越多)
     */
    @Value("${life.points.rebate.enabled:true}")
    private boolean rebateEnabled;
    @Value("${life.points.rebate.points-per-yuan:1}")
    private int rebatePerYuan;
    @Value("${life.points.rebate.tiers:50:20,100:50,300:200}")
    private String rebateTiers;

    /**
     * 消费返积分计算: 元 = 分/100 向下取整; 基础返 + 所有达到门槛的阶梯加赠累加;
     * 解析容错: 单个 tier 配置坏只跳过该档不影响整体
     */
    @Override
    public int computeConsumeRebate(int payAmountFen) {
        if (!rebateEnabled || payAmountFen <= 0) {
            return 0;
        }
        int yuan = payAmountFen / 100;
        int rebate = yuan * rebatePerYuan;
        for (String seg : rebateTiers.split(",")) {
            String[] kv = seg.trim().split(":");
            if (kv.length != 2) {
                continue;
            }
            try {
                int threshold = Integer.parseInt(kv[0].trim());
                int bonus = Integer.parseInt(kv[1].trim());
                if (yuan >= threshold) {
                    rebate += bonus;
                }
            } catch (NumberFormatException ignore) {
                log.warn("消费返积分阶梯配置解析跳过: {}", seg);
            }
        }
        return rebate;
    }

    @Override
    public PointsSummaryVO querySummary() {
        // 1. 余额以账户字段(life_user.points)为准, 不用流水求和当余额——
        //    流水是对账留痕(4.9 ReconcileTask 校验两者恒等), 冻结额单列展示(2.8)
        Long userId = BaseContext.getCurrentId();
        LifeUser user = userService.getById(userId);
        if (user == null) {
            throw new ValidationException("用户不存在");
        }

        // 2. 单SQL聚合本月流水(月初 = 当月1日00:00, create_time >= 月初 即本月):
        //    正数和 = monthEarned, 负数和的绝对值 = monthSpent; 无流水时 ifnull 已兜底为 0
        PointsSummaryVO vo = pointsRecordMapper.sumPointsSince(userId,
                LocalDate.now().withDayOfMonth(1).atStartOfDay());

        // 3. 回填余额与冻结(聚合SQL只算月度两项, total/frozen 以账户字段为准)
        vo.setTotal(user.getPoints() == null ? 0 : user.getPoints());
        vo.setFrozen(user.getFrozenPoints() == null ? 0 : user.getFrozenPoints());
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
     * <p>2.2 收编: 发放走 changePoints 统一入口, 幂等键 bizNo=RANK:yyyyMM:名次
     * (uk_biz_no 唯一索引兜底, 替代原"发放前查重"的非原子判重), 任务重跑/手动补发安全
     */
    @Override
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

        // 3. 逐名发放(奖励金额 yml 配置, 名次超出配置长度即止; 单名单事务, 互不影响)
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
            String bizNo = batch + ":" + (i + 1);
            int rankNo = i + 1;
            boolean applied = Boolean.TRUE.equals(transactionTemplate.execute(status ->
                    changePoints(winner.getUserId(), reward,
                            PointsTypeConstant.RANK_REWARD, bizNo, null,
                            lastMonth.getMonthValue() + "月积分榜第" + rankNo + "名奖励")));
            if (applied) {
                log.info("积分榜结算 {}: 第{}名 userId={} 发放 {} 分", batch, rankNo, winner.getUserId(), reward);
            }
        }
    }

    // ==================== 2.2 统一积分入口 ====================

    /**
     * 即时结算变更: 先按 bizNo 判重(幂等), 再 余额原子UPDATE + SETTLED流水 同事务落库。
     * <p>并发同 bizNo 的极端竞态由 uk_biz_no 唯一索引兜底: 后到者 INSERT 抛
     * DuplicateKeyException → 本事务整体回滚(余额变更一并撤销), 不会双入账;
     * 正向发放按 life.points.expire-days 写 expire_time(0=永久)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePoints(Long userId, int points, int type, String bizNo, String orderNo, String remark) {
        if (userId == null || points == 0) {
            return false;
        }
        if (bizNo != null && existsBizNo(bizNo)) {
            log.debug("积分变更幂等拦截 bizNo={}", bizNo);
            return false;
        }
        if (points > 0) {
            userMapper.addPoints(userId, points);
        } else if (userMapper.deductPoints(userId, -points) == 0) {
            throw new ValidationException(MessageConstant.USER_POINTS_NOT_ENOUGH);
        }
        pointsRecordMapper.insert(LifePointsRecord.builder()
                .userId(userId)
                .points(points)
                .type(type)
                .status(PointsRecordStatusConstant.SETTLED)
                .bizNo(bizNo)
                .orderNo(orderNo)
                .remark(remark)
                .expireTime(points > 0 && expireDays > 0 ? LocalDateTime.now().plusDays(expireDays) : null)
                .build());
        return true;
    }

    /**
     * 冻结(2.8): 可用→冻结 原子转移 + FROZEN 负向流水(余额未扣, 台账不计入已结算净额);
     * 可用不足抛异常由调用方事务回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean freezePoints(Long userId, int points, String bizNo, String orderNo, String remark) {
        if (userId == null || points <= 0) {
            return false;
        }
        if (bizNo != null && existsBizNo(bizNo)) {
            log.debug("积分冻结幂等拦截 bizNo={}", bizNo);
            return false;
        }
        if (userMapper.freezePoints(userId, points) == 0) {
            throw new ValidationException(MessageConstant.USER_POINTS_NOT_ENOUGH);
        }
        pointsRecordMapper.insert(LifePointsRecord.builder()
                .userId(userId)
                .points(-points)
                .type(PointsTypeConstant.CONSUME)
                .status(PointsRecordStatusConstant.FROZEN)
                .bizNo(bizNo)
                .orderNo(orderNo)
                .remark(remark)
                .build());
        return true;
    }

    /**
     * 冻结转扣减: 按 bizNo 定位 FROZEN 流水, 条件更新 status 2→1(并发只成功一次),
     * 扣冻结额; 已是终态(1/3)直接幂等返回
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean settleFrozen(String frozenBizNo) {
        return closeFrozen(frozenBizNo, PointsRecordStatusConstant.SETTLED);
    }

    /**
     * 解冻回可用: 按 bizNo 定位 FROZEN 流水, 条件更新 status 2→3, 冻结额回可用
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfreezeFrozen(String frozenBizNo) {
        return closeFrozen(frozenBizNo, PointsRecordStatusConstant.CANCELLED);
    }

    /**
     * 冻结流水终态流转公共段: 找流水→条件更新状态(CAS防并发双流转)→动账户
     */
    private boolean closeFrozen(String frozenBizNo, int targetStatus) {
        if (frozenBizNo == null) {
            return false;
        }
        LifePointsRecord record = pointsRecordMapper.selectOne(new LambdaQueryWrapper<LifePointsRecord>()
                .eq(LifePointsRecord::getBizNo, frozenBizNo)
                .last("LIMIT 1"));
        if (record == null) {
            log.warn("冻结流水不存在 bizNo={}", frozenBizNo);
            return false;
        }
        if (record.getStatus() != null && record.getStatus() != PointsRecordStatusConstant.FROZEN) {
            // 已结算/已取消: 重复调用幂等跳过
            return false;
        }
        int amount = Math.abs(record.getPoints());
        // 条件更新 status=FROZEN→目标态, 0行=被并发流转, 幂等退出
        int updated = pointsRecordMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<LifePointsRecord>()
                        .eq(LifePointsRecord::getId, record.getId())
                        .eq(LifePointsRecord::getStatus, PointsRecordStatusConstant.FROZEN)
                        .set(LifePointsRecord::getStatus, targetStatus));
        if (updated == 0) {
            return false;
        }
        if (targetStatus == PointsRecordStatusConstant.SETTLED) {
            userMapper.settleFrozen(record.getUserId(), amount);
        } else {
            userMapper.unfreezePoints(record.getUserId(), amount);
        }
        return true;
    }

    /**
     * 过期回收(2.8): 逐笔独立小事务(单笔失败不影响其余);
     * 回收额 = min(发放额, 当前余额) —— 已消耗部分豁免(无分桶模型的口径限制, 4.2 FIFO 解决);
     * 台账: 原发放行保持 SETTLED, 新增 EXPIRE 负向流水(biz_no=EXPIRE:{发放行id}),
     * "已结算流水净额 = 余额" 恒等式在部分回收下依然成立(4.9 对账依赖)
     */
    @Override
    public int reclaimExpiredPoints(int batchSize) {
        List<LifePointsRecord> candidates = pointsRecordMapper.selectExpiredCandidates(LocalDateTime.now(), batchSize);
        int reclaimed = 0;
        for (LifePointsRecord grant : candidates) {
            try {
                Boolean done = transactionTemplate.execute(status -> {
                    LifeUser user = userMapper.selectById(grant.getUserId());
                    if (user == null) {
                        return false;
                    }
                    int balance = user.getPoints() == null ? 0 : user.getPoints();
                    int amount = Math.min(grant.getPoints(), balance);
                    String bizNo = "EXPIRE:" + grant.getId();
                    if (amount > 0 && userMapper.deductPoints(grant.getUserId(), amount) == 0) {
                        // 并发消耗导致余额变化: 本轮放弃, 下轮重扫(EXPIRE流水未落, 无副作用)
                        return false;
                    }
                    pointsRecordMapper.insert(LifePointsRecord.builder()
                            .userId(grant.getUserId())
                            .points(-amount)
                            .type(PointsTypeConstant.EXPIRE)
                            .status(PointsRecordStatusConstant.SETTLED)
                            .bizNo(bizNo)
                            .remark("积分过期回收(源流水#" + grant.getId() + ")")
                            .build());
                    return true;
                });
                if (Boolean.TRUE.equals(done)) {
                    reclaimed++;
                }
            } catch (org.springframework.dao.DuplicateKeyException dup) {
                // 并发回收同一流水: uk_biz_no 拦截, 幂等跳过
                log.debug("过期回收幂等拦截 grantId={}", grant.getId());
            } catch (Exception e) {
                log.warn("过期回收失败 grantId={} (下轮重试)", grant.getId(), e);
            }
        }
        return reclaimed;
    }

    private boolean existsBizNo(String bizNo) {
        Long cnt = pointsRecordMapper.selectCount(new LambdaQueryWrapper<LifePointsRecord>()
                .eq(LifePointsRecord::getBizNo, bizNo));
        return cnt != null && cnt > 0;
    }
}

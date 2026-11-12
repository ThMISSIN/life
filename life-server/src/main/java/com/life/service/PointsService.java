package com.life.service;

import com.life.query.QueryDTO;
import com.life.vo.PointsRankVO;
import com.life.vo.PointsSummaryVO;

import java.util.List;

/**
 * 积分C端服务(均需登录) — 积分域统一入口
 * <p>2.2 已落地: 全项目积分加减一律走 changePoints / freezePoints / settleFrozen / unfreezeFrozen,
 * 余额原子UPDATE + 流水insert + bizNo唯一键幂等同事务; 禁止再直调 UserMapper.addPoints/deductPoints
 */
public interface PointsService {

    /**
     * 积分总览: 当前余额 + 冻结 + 本月获得 + 本月消耗
     */
    PointsSummaryVO querySummary();

    /**
     * 本月积分榜: 口径 = 本月正数流水求和(不是 life_user.points 余额, 余额会被消费扣减干扰且霸榜固化);
     * 实时聚合查询即最新, 无需每日刷新任务; 自然月切换榜单自动重置, 无历史月榜
     *
     * @param query pageNum/pageSize, pageSize 即取前 N 名(前端首页默认 3)
     */
    List<PointsRankVO> queryRankBoard(QueryDTO query);

    /**
     * 我的本月名次(名次 = 本月积分比我高的人数 + 1)与本月获得积分
     */
    PointsRankVO queryMyRank();

    /**
     * 月度榜单结算(定时任务入口): 结算上个自然月前三名, 发放奖励积分;
     * 自然月滚动即榜单重置, 无需清零动作
     */
    void settleMonthlyRank();

    // ==================== 2.2 统一积分入口 ====================

    /**
     * 积分变更统一入口(即时结算): 余额原子增减 + SETTLED 流水 + bizNo 幂等
     *
     * @param points 正数=发放, 负数=扣减; 扣减时余额不足抛 ValidationException(调用方回滚)
     * @param type   PointsTypeConstant
     * @param bizNo  幂等业务号(全局唯一): 已存在则整体跳过并返回 false(MQ重投/任务重放安全)
     * @return true=本次生效, false=bizNo 重复被幂等拦截
     */
    boolean changePoints(Long userId, int points, int type, String bizNo, String orderNo, String remark);

    /** 便捷重载: 无关联订单号 */
    default boolean changePoints(Long userId, int points, int type, String bizNo, String remark) {
        return changePoints(userId, points, type, bizNo, null, remark);
    }

    /**
     * 冻结积分(2.8): 可用→冻结, FROZEN 负向流水; 可用不足抛 ValidationException
     */
    boolean freezePoints(Long userId, int points, String bizNo, String orderNo, String remark);

    /**
     * 冻结转扣减(支付/发放成功): 按冻结流水 bizNo 定位, FROZEN→SETTLED; 重复调用幂等
     */
    boolean settleFrozen(String frozenBizNo);

    /**
     * 解冻回可用(取消/超时关单): 按冻结流水 bizNo 定位, FROZEN→CANCELLED; 重复调用幂等
     */
    boolean unfreezeFrozen(String frozenBizNo);

    /**
     * 过期回收(2.8 定时任务入口): 扫描 expire_time 已到的发放流水,
     * 按 min(发放额, 当前余额) 扣减并记 EXPIRE 负向流水(biz_no=EXPIRE:{发放行id} 防重)
     *
     * @return 本轮回收笔数
     */
    int reclaimExpiredPoints(int batchSize);

    /**
     * 消费力度返积分(2026-11-12): 基础返(每元 points-per-yuan 分) + 力度阶梯加赠
     * (tiers 配置 "门槛元:加赠分,..." 达档累加, 如 50:20,100:50,300:200 → 消费300元返 300+20+50+200);
     * 关闭/金额非正返回0。退款时由调用方按同公式冲回(bizNo 前缀区分)
     */
    int computeConsumeRebate(int payAmountFen);
}

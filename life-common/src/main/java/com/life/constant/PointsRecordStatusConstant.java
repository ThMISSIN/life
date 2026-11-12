package com.life.constant;

/**
 * 积分流水状态机(life_points_record.status, 2.8/3.1)
 * <p>正向发放: SETTLED(即时到账) ; 冻结链路: FROZEN → SETTLED(支付/发放成功) / CANCELLED(取消解冻);
 * 过期回收: 不改原发放行状态, 以独立负向 SETTLED 流水(type=EXPIRE, biz_no=EXPIRE:{发放行id})记账,
 * 保证"流水净额=余额"的对账恒等式在部分回收(余额不足)时也成立; EXPIRED 状态预留给未来分桶模型(4.2)
 */
public class PointsRecordStatusConstant {

    /** 已结算: 余额已生效(历史流水默认值) */
    public static final int SETTLED = 1;

    /** 冻结中: 下单冻结, 余额未扣, 待支付转扣减或取消解冻 */
    public static final int FROZEN = 2;

    /** 已取消: 冻结回滚/发放作废, 余额未受影响或已还原 */
    public static final int CANCELLED = 3;

    /** 已过期: 正向发放流水被过期回收任务核销 */
    public static final int EXPIRED = 4;
}

package com.life.constant;

/**
 * 用户权益状态(life_user_benefit.status)
 */
public class UserBenefitStatusConstant {

    /** 待生效 */
    public static final int PENDING = 0;

    /** 未使用 */
    public static final int UNUSED = 1;

    /** 已使用 */
    public static final int USED = 2;

    /** 已过期 */
    public static final int EXPIRED = 3;

    /** 已作废(不计入限兑次数) */
    public static final int VOIDED = 4;
}

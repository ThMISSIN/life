package com.life.constant;

/**
 * 权益(life_benefit)相关状态与类型
 */
public class BenefitConstant {

    /** 上架 */
    public static final int LISTED = 1;

    /** 下架 */
    public static final int UNLISTED = 0;

    /** 有效期类型: 领取后有效N天 */
    public static final int VALID_ROLLING = 1;

    /** 有效期类型: 固定截止日期 */
    public static final int VALID_FIXED = 2;
}

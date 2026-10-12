package com.life.constant;

/**
 * 连续签到奖励规则(SignRecordServiceImpl)
 */
public class SignRewardConstant {

    /** 基础签到积分(未达连续档位时) */
    public static final int BASE_POINTS = 1;

    /** 连续7天 */
    public static final int CONTINUE_DAYS_7 = 7;

    /** 连续14天 */
    public static final int CONTINUE_DAYS_14 = 14;

    /** 连续28天 */
    public static final int CONTINUE_DAYS_28 = 28;

    /** 连续7天额外奖励 */
    public static final int REWARD_POINTS_7 = 10;

    /** 连续14天额外奖励 */
    public static final int REWARD_POINTS_14 = 20;

    /** 连续28天额外奖励 */
    public static final int REWARD_POINTS_28 = 40;
}

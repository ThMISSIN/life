package com.life.constant;

/**
 * 连续签到奖励规则(SignRecordServiceImpl)
 * <p>2026-11-12: 分值改为配置化(yml life.sign.base-points / reward-7 / reward-14 / reward-28,
 * 默认 5/30/60/150, 较原 1/10/20/40 全面提高), 本类仅保留连签天数档位常量(switch case 需编译期常量)
 */
public class SignRewardConstant {

    /** 连续7天 */
    public static final int CONTINUE_DAYS_7 = 7;

    /** 连续14天 */
    public static final int CONTINUE_DAYS_14 = 14;

    /** 连续28天 */
    public static final int CONTINUE_DAYS_28 = 28;
}

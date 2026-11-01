package com.life.constant;

/**
 * 站内通知类型常量(口径: life_notice_template.type / life_user_inbox.type / life_announcement.type 前缀一致)
 */
public class NoticeTypeConstant {

    /**
     * 系统通知(公告fan-out也归此类)
     */
    public static final Integer SYSTEM = 1;

    /**
     * 订单通知
     */
    public static final Integer ORDER = 2;

    /**
     * 活动通知
     */
    public static final Integer ACTIVITY = 3;

    /**
     * 权益通知
     */
    public static final Integer BENEFIT = 4;

    /**
     * 积分通知
     */
    public static final Integer POINTS = 5;
}

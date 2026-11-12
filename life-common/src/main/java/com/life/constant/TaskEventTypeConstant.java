package com.life.constant;

/**
 * 积分任务事件类型(2.4): 与 life_points_task.type 对齐, 业务动作触发对应类型任务完成
 */
public class TaskEventTypeConstant {

    /** 每日签到(SignRecordServiceImpl 签到成功后触发) */
    public static final int SIGN = 1;

    /** 完善资料/浏览类一次性任务(用户更新资料后触发) */
    public static final int PROFILE = 2;

    /** 邀请好友(邀请体系未建, 预留无触发方) */
    public static final int INVITE = 3;

    /** 评价奖励(评论成功后触发) */
    public static final int COMMENT = 4;

    /** 分享帖子(转发成功后触发) */
    public static final int SHARE = 5;
}

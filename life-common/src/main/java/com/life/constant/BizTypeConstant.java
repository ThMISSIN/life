package com.life.constant;

/**
 * 点赞业务类型(life_liked_record.biz_type, 字符串枚举替代数字码)
 */
public class BizTypeConstant {

    /** 帖子 */
    public static final String POST = "POST";

    /** 评论 */
    public static final String COMMENT = "COMMENT";

    /**
     * 权益: life_benefit表无like_count列且永不添加, 点赞不支持此类型(白名单/回写/轮询均不得引用)
     */
    public static final String BENEFIT = "BENEFIT";

    /** 商品 */
    public static final String GOODS = "GOODS";

    /** 活动 */
    public static final String ACTIVITY = "ACTIVITY";
}

package com.life.constant;

/**
 * Redis Key 常量类
 */
public class RedisKeyConstant {

    /**
     * 用户token黑名单：用于登出后二次校验，防止旧token滥用；value为用户id，key随token剩余有效期自动过期
     */
    public static final String USER_TOKEN_BLACKLIST = "user:token:blacklist:";

    /**
     * 发帖草稿(存PostPublishDTO JSON), 占位符: {userId}
     */
    public static final String POST_DRAFT = "post:draft:uid:{userId}";

    /**
     * 草稿过期天数
     */
    public static final int POST_DRAFT_TTL_DAYS = 30;

    /**
     * 签到bitmap key前缀, 拼接: SIGN_RECORD + userId + yyyy:MM
     */
    public static final String SIGN_RECORD = "sign:uid";

    /**
     * 签到key中的月份格式
     */
    public static final String SIGN_MONTH_FORMAT = "yyyy:MM";

    /**
     * 点赞用户集合(set), 占位符: {bizType}与{bizId}(类型入key, 防多业务id串号与扫描串扰)
     */
    public static final String LIKES_BIZ = "like:set:biz:{bizType}:{bizId}";

    /**
     * 点赞集合key公共前缀, 按类型扫描时拼: LIKES_BIZ_PREFIX + bizType + ":"
     */
    public static final String LIKES_BIZ_PREFIX = "like:set:biz:";

    /**
     * 点赞计数(zset), 占位符: {bizType}
     */
    public static final String LIKES_TIMES = "likes:times:type:{bizType}";

    public static final String CART_ADD_LOCK = "cart:lock:add:{userId}:{itemType}:{itemId}";

    public static final int CART_ADD_LOCK_TTL_SECONDS = 5;

    public static final int ROW_MAX_QUANTITY = 999;


}

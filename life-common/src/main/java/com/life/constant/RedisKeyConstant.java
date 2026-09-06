package com.life.constant;

/**
 * Redis Key 常量类
 */
public class RedisKeyConstant {

    /**
     * 用户token黑名单（登出后仍在校验期内的token），value为用户id，key随token剩余有效期自动过期
     */
    public static final String USER_TOKEN_BLACKLIST = "user:token:blacklist:";

}

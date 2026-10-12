package com.life.constant;

/**
 * MQ 交换机/路由键常量(待MQ组件落地后启用)
 */
public class MqConstant {

    /** 点赞记录落库交换机 */
    public static final String LIKE_RECORD_EXCHANGE = "like.record.direct.exchange";

    /** 点赞计数消息路由键 */
    public static final String LIKE_RECORD_ROUTING_KEY = "like.record.save";
}

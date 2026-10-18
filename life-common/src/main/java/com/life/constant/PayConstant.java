package com.life.constant;

/**
 * 支付链路(life_pay_order/life_pay_channel)相关常量
 */
public class PayConstant {

    /** 渠道: 余额支付(一期模拟支付仅此渠道) */
    public static final String CHANNEL_BALANCE = "BALANCE";

    /** 支付单状态: 0-待支付 */
    public static final int PAY_WAITING = 0;

    /** 支付单状态: 2-支付成功 */
    public static final int PAY_SUCCESS = 2;
}

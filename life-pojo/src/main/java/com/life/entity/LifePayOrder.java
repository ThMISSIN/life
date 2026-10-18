package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支付订单表: 与业务订单1:1(biz_order_no唯一键), 渠道/状态解耦于 life_order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_pay_order")
public class LifePayOrder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务订单号(life_order.order_no)
     */
    private String bizOrderNo;

    /**
     * 支付单号
     */
    private String payOrderNo;

    /**
     * 支付金额(分)
     */
    private Integer totalAmount;

    /**
     * 积分支付部分(积分抵扣上线后启用)
     */
    private Integer pointsAmount;

    /**
     * 状态: 0-待支付, 1-支付中, 2-支付成功, 3-支付失败, 4-已关闭
     */
    private Integer status;

    /**
     * 支付渠道编码(life_pay_channel.code)
     */
    private String channelCode;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 第三方交易号(模拟支付为假号, 真实渠道接入后回填)
     */
    private String tradeNo;

    /**
     * 二维码(线上渠道预下单用)
     */
    private String qrCode;

    /**
     * 回调地址
     */
    private String notifyUrl;
}

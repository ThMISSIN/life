package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付渠道(2.6): 启用表驱动渠道读取, 当前仅 BALANCE 真实可用,
 * WECHAT/ALIPAY 待商户资质接入后开放(下单/支付时校验渠道 status=1)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_pay_channel")
public class LifePayChannel extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 渠道编码: WECHAT/ALIPAY/POINTS/BALANCE (uk_code) */
    private String code;

    /** 渠道类型: 1-第三方支付, 2-余额支付, 3-积分支付 */
    private Integer channelType;

    private Integer priority;

    private String icon;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;
}

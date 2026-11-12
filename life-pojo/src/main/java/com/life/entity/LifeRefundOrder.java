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
 * 退款单(2.5 管理端退款): uk_biz_order_no 一单一退幂等;
 * 一期余额渠道同步退款成功(status 0→2), 真实第三方渠道接入后走 0-待退款→回调→2/3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_refund_order")
public class LifeRefundOrder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务订单号 */
    private String bizOrderNo;

    /** 退款单号(RF+时间戳序列) */
    private String refundNo;

    /** 退款金额(分) */
    private Integer refundAmount;

    /** 状态: 0-待退款, 1-退款中, 2-退款成功, 3-退款失败 */
    private Integer status;

    /** 退款渠道编码(life_pay_channel.code) */
    private String channelCode;

    /** 退款原因 */
    private String reason;

    private LocalDateTime refundTime;

    /** 第三方退款流水号(余额渠道为 MOCK 占位) */
    private String tradeNo;
}

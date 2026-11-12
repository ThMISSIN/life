package com.life.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * MQ站内通知报文(2.12): 业务事件(兑换成功/支付成功/退款) → notice.event.queue → NoticeService.sendNotice;
 * msgId 为消费端幂等键(Redis setnx 去重, 防重投双写收件箱/双推送)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqNoticeMessage implements Serializable {

    /** 消息唯一id(UUID), 消费幂等键 */
    private String msgId;

    private Long userId;

    /** 通知类型, 见 NoticeTypeConstant */
    private Integer type;

    private String title;

    private String content;

    /** 业务ID(可空): 订单id/权益id等 */
    private Long bizId;

    /** 前端跳转链接(可空) */
    private String url;
}

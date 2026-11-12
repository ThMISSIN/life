package com.life.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * MQ延迟关单报文(2.12): 下单时投递到 TTL 队列, 到期死信转发 order.close.queue;
 * 消费端条件更新 status 0→4(已支付/已取消自然不命中), 与扫描兜底任务幂等共存
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqOrderCloseMessage implements Serializable {

    private Long orderId;

    private String orderNo;
}

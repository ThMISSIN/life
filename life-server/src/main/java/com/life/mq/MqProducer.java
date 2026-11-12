package com.life.mq;

import com.life.constant.MqConstant;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MQ 生产端统一封装(2.12): traceId 随消息头透传(4.10), 发送策略分两档——
 * send: 失败上抛(签到等"发放必须可达"链路, broker 不可用即业务失败, 与既有口径一致);
 * sendQuiet: 失败仅告警(通知/延迟关单等有兜底或软态链路, 不因MQ故障阻断主业务)
 */
@Component
@Slf4j
public class MqProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(String exchange, String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
            String traceId = MDC.get(TraceIdHolder.TRACE_ID);
            if (traceId != null) {
                message.getMessageProperties().setHeader(MqConstant.HEADER_TRACE_ID, traceId);
            }
            return message;
        });
    }

    public boolean sendQuiet(String exchange, String routingKey, Object payload) {
        try {
            send(exchange, routingKey, payload);
            return true;
        } catch (Exception e) {
            log.warn("MQ发送失败(静默) exchange={} rk={}", exchange, routingKey, e);
            return false;
        }
    }

    /**
     * 延迟消息(消息级 expiration): 投递到无消费者的 TTL 队列, 到期由队列 DLX 转发到目标队列;
     * 同队列同TTL下 FIFO 到期, 无队头阻塞畸变
     */
    public boolean sendDelay(String exchange, String routingKey, Object payload, long delayMillis) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
                String traceId = MDC.get(TraceIdHolder.TRACE_ID);
                if (traceId != null) {
                    message.getMessageProperties().setHeader(MqConstant.HEADER_TRACE_ID, traceId);
                }
                message.getMessageProperties().setExpiration(String.valueOf(delayMillis));
                return message;
            });
            return true;
        } catch (Exception e) {
            log.warn("MQ延迟消息发送失败(扫描任务兜底) exchange={} rk={}", exchange, routingKey, e);
            return false;
        }
    }
}

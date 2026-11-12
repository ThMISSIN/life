package com.life.listener;

import com.life.constant.MqConstant;
import com.life.dto.MqOrderCloseMessage;
import com.life.mq.TraceIdHolder;
import com.life.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 延迟关单 MQ 消费者(2.5/2.12): 下单时投递 TTL 消息, 到期后本监听器触发关单;
 * closeOrderIfPending 条件更新 status 0→4 —— 已支付/已取消订单自然不命中,
 * 与 OrderCloseTask 扫描兜底幂等共存(先到的执行, 后到的0行跳过)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCloseListener {

    private final OrderService orderService;

    @RabbitListener(queues = MqConstant.ORDER_CLOSE_QUEUE)
    public void onOrderClose(MqOrderCloseMessage msg, Message raw, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        MqTraceSupport.restore(raw);
        try {
            orderService.closeOrderIfPending(msg.getOrderId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("延迟关单消费失败, 转死信(扫描任务兜底) orderId={}", msg.getOrderId(), e);
            channel.basicNack(tag, false, false);
        } finally {
            MDC.remove(TraceIdHolder.TRACE_ID);
        }
    }
}

package com.life.listener;

import com.life.constant.MqConstant;
import com.life.dto.MqPointsMessage;
import com.life.mq.TraceIdHolder;
import com.life.service.LifePointsRecordService;
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
 * 签到积分 MQ 消费者(与 SignRecordServiceImpl 的 convertAndSend 配对)
 * <p>链路: 签到 → points.direct / sign.in → sign.point.queue → 本监听器
 * → LifePointsRecordService.addSignRecord → PointsService.changePoints(入账+流水同事务)
 * <p>2.12 升级: 手动ack(业务异常 nack 不重投, 进死信队列留痕);
 * at-least-once 重投的重复入账由 bizNo=SIGN:{userId}:{yyyyMMdd} 唯一索引幂等拦截
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AddPointsListener {

    private final LifePointsRecordService pointsRecordService;

    @RabbitListener(queues = MqConstant.POINTS_SIGN_QUEUE)
    public void listenSignInMessage(MqPointsMessage msg, Message raw, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        MqTraceSupport.restore(raw);
        try {
            pointsRecordService.addSignRecord(msg.getUserId(), msg.getPoints(), msg.getBizNo());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("签到积分消费失败, 转死信 bizNo={}", msg.getBizNo(), e);
            channel.basicNack(tag, false, false);
        } finally {
            MDC.remove(TraceIdHolder.TRACE_ID);
        }
    }
}

package com.life.listener;

import com.life.constant.MqConstant;
import com.life.constant.PointsTypeConstant;
import com.life.dto.MqPointsMessage;
import com.life.mq.TraceIdHolder;
import com.life.service.PointsService;
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
 * 发帖积分 MQ 消费者(2026-11-12): 发帖/审核过审 → points.direct / post.reward → 本监听器;
 * bizNo=POST:{postId} 唯一索引幂等 —— 发布与过审双触发/消息重投都只入账一次
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostPointsListener {

    private final PointsService pointsService;

    @RabbitListener(queues = MqConstant.POINTS_POST_QUEUE)
    public void onPostReward(MqPointsMessage msg, Message raw, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        MqTraceSupport.restore(raw);
        try {
            pointsService.changePoints(msg.getUserId(), msg.getPoints(),
                    PointsTypeConstant.ACTIVITY, msg.getBizNo(), msg.getRemark());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("发帖奖励发放失败, 转死信 bizNo={}", msg.getBizNo(), e);
            channel.basicNack(tag, false, false);
        } finally {
            MDC.remove(TraceIdHolder.TRACE_ID);
        }
    }
}

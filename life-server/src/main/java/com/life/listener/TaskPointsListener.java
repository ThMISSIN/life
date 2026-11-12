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
 * 任务奖励 MQ 消费者(2.4/2.12): 领取动作同步落 task_record, 积分发放异步化;
 * bizNo=TASKCLAIM:{recordId} 唯一索引幂等, 重投不双发
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskPointsListener {

    private final PointsService pointsService;

    @RabbitListener(queues = MqConstant.POINTS_TASK_QUEUE)
    public void onTaskReward(MqPointsMessage msg, Message raw, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        MqTraceSupport.restore(raw);
        try {
            pointsService.changePoints(msg.getUserId(), msg.getPoints(),
                    PointsTypeConstant.ACTIVITY, msg.getBizNo(), msg.getRemark());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("任务奖励发放失败, 转死信 bizNo={}", msg.getBizNo(), e);
            channel.basicNack(tag, false, false);
        } finally {
            MDC.remove(TraceIdHolder.TRACE_ID);
        }
    }
}

package com.life.listener;

import com.life.constant.MqConstant;
import com.life.constant.RedisConstant;
import com.life.dto.MqNoticeMessage;
import com.life.mq.TraceIdHolder;
import com.life.service.NoticeService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * 站内通知 MQ 消费者(2.12): 兑换成功/支付成功/退款等业务通知事件 → 收件箱落库 + WS 实时推送;
 * msgId Redis setnx 去重(at-least-once 重投不双写); 公告 fan-out 仍走 Redis Pub/Sub 跨进程通道(见 NoticeEventListener)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NoticeMqListener {

    private final NoticeService noticeService;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = MqConstant.NOTICE_QUEUE)
    public void onNotice(MqNoticeMessage msg, Message raw, Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        MqTraceSupport.restore(raw);
        try {
            String dedupKey = RedisConstant.MQ_NOTICE_DEDUP.replace("{msgId}", msg.getMsgId());
            Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofDays(1));
            if (Boolean.TRUE.equals(first)) {
                noticeService.sendNotice(msg.getUserId(), msg.getType(), msg.getTitle(),
                        msg.getContent(), msg.getBizId(), msg.getUrl());
            } else {
                log.debug("通知消息重复投递, 幂等跳过 msgId={}", msg.getMsgId());
            }
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("通知事件消费失败, 转死信 msgId={}", msg.getMsgId(), e);
            channel.basicNack(tag, false, false);
        } finally {
            MDC.remove(TraceIdHolder.TRACE_ID);
        }
    }
}

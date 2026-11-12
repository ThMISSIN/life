package com.life.listener;

import com.life.constant.BizTypeConstant;
import com.life.constant.MqConstant;
import com.life.dto.LikedTimesDTO;
import com.life.dto.MqLikeBatchMessage;
import com.life.mapper.LifeCommentMapper;
import com.life.mapper.LifePostMapper;
import com.life.mq.TraceIdHolder;
import com.life.service.PostHotService;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 点赞记录回写 MQ 消费者(2.12): 回写任务排空 ZSet 后整批投递 → 本监听器落库;
 * score 是点赞SET size 终值, 整写覆盖幂等, 重投不漂移; 帖子顺路批量刷热度(2.11 搭车口径迁移至此)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LikeRecordListener {

    private final LifePostMapper postMapper;
    private final LifeCommentMapper commentMapper;
    private final PostHotService postHotService;

    @RabbitListener(queues = MqConstant.LIKE_RECORD_QUEUE)
    public void onLikeBatch(MqLikeBatchMessage msg, Message raw, Channel channel,
                            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        MqTraceSupport.restore(raw);
        try {
            List<Long> hotPostIds = null;
            for (LikedTimesDTO dto : msg.getItems()) {
                if (BizTypeConstant.POST.equals(msg.getBizType())) {
                    postMapper.updateLikeCount(dto.getBizId(), dto.getLikedTimes());
                    if (hotPostIds == null) {
                        hotPostIds = new ArrayList<>(msg.getItems().size());
                    }
                    hotPostIds.add(dto.getBizId());
                } else if (BizTypeConstant.COMMENT.equals(msg.getBizType())) {
                    commentMapper.updateLikeCount(dto.getBizId(), dto.getLikedTimes());
                }
            }
            if (hotPostIds != null) {
                postHotService.recalcBatch(hotPostIds);
            }
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("点赞回写消费失败, 转死信 bizType={} size={}", msg.getBizType(),
                    msg.getItems() == null ? 0 : msg.getItems().size(), e);
            channel.basicNack(tag, false, false);
        } finally {
            MDC.remove(TraceIdHolder.TRACE_ID);
        }
    }
}

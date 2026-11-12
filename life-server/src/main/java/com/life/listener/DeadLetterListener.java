package com.life.listener;

import com.life.constant.MqConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 死信留痕(2.12): 各业务队列 nack(requeue=false)/TTL到期(非关单链路) 的消息汇聚于此,
 * 记录原始报文与死信原因(x-death header), 供人工排查与补偿; 不自动重放(防毒消息循环)
 */
@Component
@Slf4j
public class DeadLetterListener {

    @RabbitListener(queues = MqConstant.DEAD_QUEUE)
    public void onDeadLetter(Message message) {
        String body = message.getBody() == null ? "" : new String(message.getBody(), StandardCharsets.UTF_8);
        List<Map<String, ?>> deaths = message.getMessageProperties().getXDeathHeader();
        String reason = deaths == null || deaths.isEmpty() ? "unknown" : String.valueOf(deaths.get(0).get("reason"));
        String queue = deaths == null || deaths.isEmpty() ? "unknown" : String.valueOf(deaths.get(0).get("queue"));
        log.error("[死信] queue={} reason={} body={}", queue, reason, body);
    }
}

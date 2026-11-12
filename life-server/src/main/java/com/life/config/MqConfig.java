package com.life.config;

import com.life.constant.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * 2.12 RabbitMQ 全链路拓扑声明
 * <p>约定: 所有业务队列挂死信(x-dead-letter-exchange=life.dlx.exchange), 消费端手动ack,
 * 业务异常 nack(requeue=false) 进 life.dead.queue 留痕, 毒消息不阻塞主队列;
 * 延迟关单用 "TTL队列(无消费者) + DLX转发" 实现(消息级 expiration, 同TTL队列内FIFO到期,
 * 不依赖 delayed-message 插件); 队列参数固定, 配置变化只影响消息TTL不触发 PRECONDITION_FAILED
 */
@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class MqConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ==================== 死信 ====================

    @Bean
    public DirectExchange lifeDlxExchange() {
        return new DirectExchange(MqConstant.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue lifeDeadQueue() {
        return QueueBuilder.durable(MqConstant.DEAD_QUEUE).build();
    }

    @Bean
    public Binding lifeDeadBinding() {
        return BindingBuilder.bind(lifeDeadQueue()).to(lifeDlxExchange()).with(MqConstant.DEAD_ROUTING_KEY);
    }

    /** 业务队列统一构建: durable + 死信转发 */
    private Queue durableWithDlx(String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(MqConstant.DLX_EXCHANGE)
                .deadLetterRoutingKey(MqConstant.DEAD_ROUTING_KEY)
                .build();
    }

    // ==================== 积分发放(签到/任务奖励) ====================

    @Bean
    public DirectExchange pointsExchange() {
        return new DirectExchange(MqConstant.POINTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue signPointQueue() {
        return durableWithDlx(MqConstant.POINTS_SIGN_QUEUE);
    }

    @Bean
    public Binding signPointBinding() {
        return BindingBuilder.bind(signPointQueue()).to(pointsExchange()).with(MqConstant.POINTS_SIGN_ROUTING_KEY);
    }

    @Bean
    public Queue taskPointQueue() {
        return durableWithDlx(MqConstant.POINTS_TASK_QUEUE);
    }

    @Bean
    public Binding taskPointBinding() {
        return BindingBuilder.bind(taskPointQueue()).to(pointsExchange()).with(MqConstant.POINTS_TASK_ROUTING_KEY);
    }

    @Bean
    public Queue postPointQueue() {
        return durableWithDlx(MqConstant.POINTS_POST_QUEUE);
    }

    @Bean
    public Binding postPointBinding() {
        return BindingBuilder.bind(postPointQueue()).to(pointsExchange()).with(MqConstant.POINTS_POST_ROUTING_KEY);
    }

    // ==================== 点赞记录落库 ====================

    @Bean
    public DirectExchange likeRecordExchange() {
        return new DirectExchange(MqConstant.LIKE_RECORD_EXCHANGE, true, false);
    }

    @Bean
    public Queue likeRecordQueue() {
        return durableWithDlx(MqConstant.LIKE_RECORD_QUEUE);
    }

    @Bean
    public Binding likeRecordBinding() {
        return BindingBuilder.bind(likeRecordQueue()).to(likeRecordExchange()).with(MqConstant.LIKE_RECORD_ROUTING_KEY);
    }

    // ==================== 站内通知事件 ====================

    @Bean
    public DirectExchange noticeExchange() {
        return new DirectExchange(MqConstant.NOTICE_EXCHANGE, true, false);
    }

    @Bean
    public Queue noticeQueue() {
        return durableWithDlx(MqConstant.NOTICE_QUEUE);
    }

    @Bean
    public Binding noticeBinding() {
        return BindingBuilder.bind(noticeQueue()).to(noticeExchange()).with(MqConstant.NOTICE_ROUTING_KEY);
    }

    // ==================== 订单延迟关单(TTL + DLX) ====================

    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(MqConstant.ORDER_DELAY_EXCHANGE, true, false);
    }

    /**
     * 延迟队列: 无消费者, 消息到期(消息级expiration=关单时限)死信转发到 order.close.queue;
     * 不挂业务死信(它的DLX就是关单交换机)
     */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(MqConstant.ORDER_DELAY_QUEUE)
                .deadLetterExchange(MqConstant.ORDER_CLOSE_EXCHANGE)
                .deadLetterRoutingKey(MqConstant.ORDER_CLOSE_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(MqConstant.ORDER_DELAY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderCloseExchange() {
        return new DirectExchange(MqConstant.ORDER_CLOSE_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCloseQueue() {
        return durableWithDlx(MqConstant.ORDER_CLOSE_QUEUE);
    }

    @Bean
    public Binding orderCloseBinding() {
        return BindingBuilder.bind(orderCloseQueue()).to(orderCloseExchange()).with(MqConstant.ORDER_CLOSE_ROUTING_KEY);
    }
}

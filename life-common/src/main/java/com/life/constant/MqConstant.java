package com.life.constant;

/**
 * MQ 拓扑常量(2.12 全链路): 业务队列统一挂死信(life.dlx.exchange → life.dead.queue),
 * 消费端手动 ack, 业务异常 nack(requeue=false) 进死信留痕, 毒消息不阻塞主队列
 */
public class MqConstant {

    // ===== 死信 =====
    /** 死信交换机(所有业务队列的 x-dead-letter-exchange) */
    public static final String DLX_EXCHANGE = "life.dlx.exchange";
    /** 死信队列: 仅日志留痕+管理端排查, 不自动重放 */
    public static final String DEAD_QUEUE = "life.dead.queue";
    /** 死信路由键(各业务队列 x-dead-letter-routing-key 统一指向) */
    public static final String DEAD_ROUTING_KEY = "life.dead";

    // ===== 积分发放(签到/任务奖励) =====
    public static final String POINTS_EXCHANGE = "points.direct";
    public static final String POINTS_SIGN_ROUTING_KEY = "sign.in";
    public static final String POINTS_SIGN_QUEUE = "sign.point.queue";
    public static final String POINTS_TASK_ROUTING_KEY = "task.reward";
    public static final String POINTS_TASK_QUEUE = "task.point.queue";
    /** 发帖奖励(2026-11-12): 发帖/审核过审后投递, bizNo=POST:{postId} 幂等 */
    public static final String POINTS_POST_ROUTING_KEY = "post.reward";
    public static final String POINTS_POST_QUEUE = "post.point.queue";

    // ===== 点赞记录落库(回写任务排空ZSet后投递, 替代同步写库) =====
    public static final String LIKE_RECORD_EXCHANGE = "like.record.direct.exchange";
    public static final String LIKE_RECORD_ROUTING_KEY = "like.record.save";
    public static final String LIKE_RECORD_QUEUE = "like.record.queue";

    // ===== 站内通知事件(兑换成功/支付成功/退款等业务通知) =====
    public static final String NOTICE_EXCHANGE = "notice.event.direct";
    public static final String NOTICE_ROUTING_KEY = "notice.send";
    public static final String NOTICE_QUEUE = "notice.event.queue";

    // ===== 订单延迟关单(TTL+DLX 实现延迟消息: delay队列无消费者, 到期死信转发到close队列) =====
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.direct";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.close.delay";
    public static final String ORDER_DELAY_QUEUE = "order.close.delay.queue";
    public static final String ORDER_CLOSE_EXCHANGE = "order.close.direct";
    public static final String ORDER_CLOSE_ROUTING_KEY = "order.close";
    public static final String ORDER_CLOSE_QUEUE = "order.close.queue";

    /** 消息头: 链路追踪id(4.10) */
    public static final String HEADER_TRACE_ID = "traceId";
}

package com.life.listener;

import com.life.service.LifePointsRecordService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 签到积分 MQ 消费者(与 SignRecordServiceImpl 的 convertAndSend 配对)
 * <p>
 * 链路: 签到 → points.direct / sign.in → sign.point.queue → 本监听器
 * → LifePointsRecordService.addSignRecord(入账+流水同事务, 2026-11-01 修复不入账缺陷)
 * <p>
 * 修复(2026-11-01): 原签名 (Long userId, int points) 与生产端 Map 报文不匹配
 * (Jackson2Json 转换器下无法绑定, 消息会进死循环重投), 且 pointsRecordService 未注入
 * (绑定成功也会 NPE); 现按 Map 报文取值。
 * <p>
 * 注意: MQ 为 at-least-once, 消息重投会重复入账 —— 幂等键(order_no=SIGN:userId:日期 唯一索引)
 * 随路线图 2.12 补强。
 */
@Component
public class AddPointsListener {

    private final LifePointsRecordService pointsRecordService;

    public AddPointsListener(LifePointsRecordService pointsRecordService) {
        this.pointsRecordService = pointsRecordService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "sign.point.queue", durable = "true"),
            exchange = @Exchange(name = "points.direct"),
            key = "sign.in"
    )
    )
    public void listenSignInMessage(Map<String, Object> msg) {
        Long userId = Long.valueOf(String.valueOf(msg.get("userId")));
        int points = Integer.parseInt(String.valueOf(msg.get("points")));
        pointsRecordService.addSignRecord(userId, points);
    }

}

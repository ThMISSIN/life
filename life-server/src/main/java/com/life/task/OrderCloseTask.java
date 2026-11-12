package com.life.task;

import com.life.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 2.5 超时未支付自动关单(扫描兜底): 主链路是下单时投递的 TTL 延迟消息(OrderCloseListener),
 * 本任务收敛 broker 宕机/消息丢失期间的漏网单; 两者共用 closeOrderIfPending 的 CAS 幂等入口。
 * 一期 @Scheduled + 分布式锁, 二期随 2.15 迁 XXL-Job
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCloseTask {

    private final OrderService orderService;
    private final TaskLockSupport taskLock;

    /**
     * 每5分钟扫描一轮(时限口径 life.order.auto-close-minutes, 默认30分钟)
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 90_000)
    public void closeTimeoutOrders() {
        taskLock.runWithLock("orderClose", () -> {
            try {
                orderService.closeTimeoutOrders();
            } catch (Exception e) {
                log.warn("超时关单扫描异常(下轮自愈)", e);
            }
        });
    }
}

package com.life.task;

import com.life.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 超时自动确认收货任务(2.5 配套): 每 10 分钟扫 status=2 且发货超 N 天的订单批量置3。
 * <p>
 * N 由 life.order.auto-confirm-days 配置(默认7天); 发货时间以 update_time 近似,
 * 二期随 2.15 XXL-Job 统一调度并补 deliver_time 列精确化;
 * 4.7 起经 TaskLockSupport 分布式锁, 多实例部署只有一台执行
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoConfirmTask {

    private final OrderService orderService;
    private final TaskLockSupport taskLock;

    @Scheduled(cron = "0 */10 * * * ?")
    public void autoConfirm() {
        taskLock.runWithLock("orderAutoConfirm", () -> {
            int n = orderService.autoConfirmTimeoutOrders();
            if (n > 0) {
                log.info("超时自动确认收货: {} 单", n);
            }
        });
    }
}

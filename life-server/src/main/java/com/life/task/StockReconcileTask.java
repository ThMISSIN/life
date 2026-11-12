package com.life.task;

import com.life.service.StockCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 2.14 库存对账: 每10分钟遍历预热注册表, Redis 镜像 ≠ DB 库存 → 以 DB 为准重置 + 告警日志;
 * 覆盖补偿回补失败/管理端(RuoYi)直改DB 造成的漂移, 保证 Lua 预扣层不误杀/不漏杀
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockReconcileTask {

    private final StockCacheService stockCacheService;
    private final TaskLockSupport taskLock;

    @Scheduled(fixedDelay = 600_000, initialDelay = 120_000)
    public void reconcile() {
        taskLock.runWithLock("stockReconcile", () -> {
            try {
                stockCacheService.reconcile();
            } catch (Exception e) {
                log.warn("库存对账异常(下轮自愈)", e);
            }
        });
    }
}

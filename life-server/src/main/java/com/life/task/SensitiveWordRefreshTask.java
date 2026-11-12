package com.life.task;

import com.life.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 2.9 敏感词词典定时刷新: 管理端(RuoYi/REST)改词后本实例即时 refresh,
 * 其余实例靠本任务 5 分钟收敛; 词典在内存(volatile 整树替换), 刷新不加分布式锁——
 * 每个实例都必须各自持有最新词典
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SensitiveWordRefreshTask {

    private final SensitiveWordService sensitiveWordService;

    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    public void refresh() {
        try {
            sensitiveWordService.refresh();
        } catch (Exception e) {
            log.warn("敏感词词典刷新异常(沿用旧词典)", e);
        }
    }
}

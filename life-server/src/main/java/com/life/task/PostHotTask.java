package com.life.task;

import com.life.service.PostHotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 2.11 热度链路定时任务
 * <p>点赞的热度刷新不在本类: 搭点赞回写链路的车(LikeRecordListener 消费端, 2.12 后落库与刷热度同在消费侧);
 * 这里承担另外两件事——阅读排空回写(同点赞的Redis累计-回写模式)与热度衰减扫描。
 * 4.7 起经 TaskLockSupport 分布式锁: 阅读增量排空非幂等(range+remove), 多实例重复执行会丢/重计数, 必须单发。
 * 二期随 2.15 迁 XXL-Job 统一调度
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PostHotTask {

    private final PostHotService postHotService;
    private final TaskLockSupport taskLock;

    /**
     * 阅读增量回写: 排空 post:view:times → view_count 原子累加 → 批量刷热度。
     * 周期与点赞回写任务一致(20s)
     */
    @Scheduled(fixedDelay = 20_000)
    public void drainViewTimes() {
        taskLock.runWithLock("postViewDrain", () -> {
            try {
                int n = postHotService.drainViewTimes();
                if (n > 0) {
                    log.debug("阅读回写完成 {} 帖", n);
                }
            } catch (Exception e) {
                log.warn("阅读回写任务异常(下轮自愈)", e);
            }
        });
    }

    /**
     * 热度衰减扫描: 每10分钟重算 recalcWindowHours 窗口内的已发布帖——
     * 时间衰减持续生效, 无互动的帖自然下沉, 新帖24h扶持到期自动摘除
     */
    @Scheduled(fixedDelay = 600_000, initialDelay = 30_000)
    public void sweepHotDecay() {
        taskLock.runWithLock("postHotSweep", () -> {
            try {
                postHotService.sweepRecentPosts();
            } catch (Exception e) {
                log.warn("热度衰减扫描异常(下轮自愈)", e);
            }
        });
    }
}

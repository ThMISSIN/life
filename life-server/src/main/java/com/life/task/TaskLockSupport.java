package com.life.task;

import com.life.constant.RedisConstant;
import com.life.mq.TraceIdHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 4.7 定时任务分布式锁包装: 多实例部署时 @Scheduled 会每实例各跑一遍(重复回写/重复结算),
 * 统一经本包装 Redisson tryLock(waitTime=0, 看门狗续期) —— 抢到锁的实例执行, 其余静默跳过本轮;
 * 顺带为每轮执行生成 traceId(4.10), 任务日志可串联
 * <p>2.15 XXL-Job 接入后本类废弃(调度中心天然单发), 现阶段为多实例正确性的最小实现
 */
@Component
@Slf4j
public class TaskLockSupport {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("MMddHHmmss");

    @Autowired
    private RedissonClient redissonClient;

    /**
     * @return true=本实例执行了任务体, false=锁被其他实例持有跳过
     */
    public boolean runWithLock(String taskName, Runnable body) {
        RLock lock = redissonClient.getLock(RedisConstant.TASK_LOCK.replace("{taskName}", taskName));
        boolean locked = false;
        try {
            locked = lock.tryLock();
        } catch (Exception e) {
            // Redis 异常时放行执行(单机部署不受影响; 多实例极端情况下重复执行由各任务幂等兜底)
            log.warn("任务锁获取异常, 本轮直接执行 task={}", taskName, e);
            locked = false;
            MDC.put(TraceIdHolder.TRACE_ID, taskName + "-" + LocalDateTime.now().format(TS_FMT));
            try {
                body.run();
            } finally {
                MDC.remove(TraceIdHolder.TRACE_ID);
            }
            return true;
        }
        if (!locked) {
            log.debug("任务锁被其他实例持有, 跳过本轮 task={}", taskName);
            return false;
        }
        MDC.put(TraceIdHolder.TRACE_ID, taskName + "-" + LocalDateTime.now().format(TS_FMT));
        try {
            body.run();
            return true;
        } finally {
            MDC.remove(TraceIdHolder.TRACE_ID);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}

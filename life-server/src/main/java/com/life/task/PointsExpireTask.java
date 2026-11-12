package com.life.task;

import com.life.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 2.8 积分过期回收: 每日 03:30 扫描 expire_time 已到的发放流水,
 * 按 min(发放额, 当前余额) 扣减并记 EXPIRE 负向流水(biz_no=EXPIRE:{id} 幂等);
 * life.points.expire-days=0(默认)时发放不写 expire_time, 本任务天然空转
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PointsExpireTask {

    private final PointsService pointsService;
    private final TaskLockSupport taskLock;

    @Scheduled(cron = "0 30 3 * * ?")
    public void reclaimExpired() {
        taskLock.runWithLock("pointsExpire", () -> {
            try {
                int n = pointsService.reclaimExpiredPoints(500);
                if (n > 0) {
                    log.info("积分过期回收完成: {} 笔", n);
                }
            } catch (Exception e) {
                log.warn("积分过期回收异常(次日重试)", e);
            }
        });
    }
}

package com.life.task;

import com.life.mapper.LifeUserBenefitMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 5.2 权益过期落库: 每日 03:10 将 UNUSED 且 expire_time 已到的券批量置 EXPIRED(3),
 * 修复"过期仅查询时实时推导, 报表/对账口径不一致"隐患; 展示层实时修正逻辑保留兼容
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BenefitExpireTask {

    private final LifeUserBenefitMapper userBenefitMapper;
    private final TaskLockSupport taskLock;

    @Scheduled(cron = "0 10 3 * * ?")
    public void markExpired() {
        taskLock.runWithLock("benefitExpire", () -> {
            try {
                int n = userBenefitMapper.markExpired();
                if (n > 0) {
                    log.info("权益过期落库: {} 张", n);
                }
            } catch (Exception e) {
                log.warn("权益过期落库异常(次日重试)", e);
            }
        });
    }
}

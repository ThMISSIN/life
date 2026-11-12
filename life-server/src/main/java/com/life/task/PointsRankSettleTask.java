package com.life.task;

import com.life.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 积分榜单月度结算任务: 每月 1 号 01:00 结算上个自然月前三名, 发放奖励积分
 *
 * @EnableScheduling 已在 LifeApplication 打开(2026-11-02), 本任务与点赞回写/超时自动确认一并生效;
 * 结算幂等由 changePoints bizNo="RANK:yyyyMM:名次"(uk_biz_no) 保证, 任务重跑/手动补发安全;
 * 4.7 起经 TaskLockSupport 分布式锁, 多实例部署只有一台执行
 */
@Component
@RequiredArgsConstructor
public class PointsRankSettleTask {

    private final PointsService pointsService;
    private final TaskLockSupport taskLock;

    @Scheduled(cron = "0 0 1 1 * ?")
    public void settleLastMonthRank() {
        taskLock.runWithLock("pointsRankSettle", () -> pointsService.settleMonthlyRank());
    }
}

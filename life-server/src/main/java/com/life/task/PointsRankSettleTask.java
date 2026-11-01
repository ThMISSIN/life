package com.life.task;

import com.life.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 积分榜单月度结算任务: 每月 1 号 01:00 结算上个自然月前三名, 发放奖励积分
 *
 * 启用前提: @EnableScheduling 目前在 LifeApplication 上是注释状态, 需先打开;
 * 注意打开调度会同时激活 LikedTimesCheckTask(20s 一次的点赞数同步), 若暂不想启用它,
 * 先单独注释其类上的 @Component 或 @Scheduled, 避免两个任务一起生效
 */
@Component
@RequiredArgsConstructor
public class PointsRankSettleTask {

    private final PointsService pointsService;

    @Scheduled(cron = "0 0 1 1 * ?")
    public void settleLastMonthRank() {
        pointsService.settleMonthlyRank();
    }
}

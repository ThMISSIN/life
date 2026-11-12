package com.life.task;

import com.life.entity.LifeUser;
import com.life.mapper.LifeBalanceRecordMapper;
import com.life.mapper.LifePointsRecordMapper;
import com.life.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 4.9 对账中心(轻量版): 每日 04:00 逐用户校验两条恒等式——
 * ① 积分: SUM(已结算流水) == life_user.points ; ② 资金: SUM(余额流水) == life_user.money。
 * 差异仅告警日志留痕([对账] 前缀可被日志采集规则捕获), 不自动改账——差异根因需人工判定
 * (历史脏数据/口径变更/真实资损), 自动"修复"反而抹掉证据
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReconcileTask {

    private final UserMapper userMapper;
    private final LifePointsRecordMapper pointsRecordMapper;
    private final LifeBalanceRecordMapper balanceRecordMapper;
    private final TaskLockSupport taskLock;

    @Scheduled(cron = "0 0 4 * * ?")
    public void reconcile() {
        taskLock.runWithLock("reconcile", () -> {
            try {
                List<LifeUser> users = userMapper.selectList(new LambdaQueryWrapper<LifeUser>()
                        .select(LifeUser::getId, LifeUser::getUsername, LifeUser::getPoints, LifeUser::getMoney));
                int pointsDiff = 0;
                int moneyDiff = 0;
                for (LifeUser u : users) {
                    Long pointsSum = pointsRecordMapper.sumSettledPoints(u.getId());
                    int balance = u.getPoints() == null ? 0 : u.getPoints();
                    if (pointsSum != null && pointsSum.longValue() != balance) {
                        pointsDiff++;
                        log.warn("[对账] 积分不平 userId={} username={} 流水净额={} 账户余额={}",
                                u.getId(), u.getUsername(), pointsSum, balance);
                    }
                    Long moneySum = balanceRecordMapper.sumAmount(u.getId());
                    int money = u.getMoney() == null ? 0 : u.getMoney();
                    if (moneySum != null && moneySum.longValue() != money) {
                        moneyDiff++;
                        log.warn("[对账] 资金不平 userId={} username={} 流水净额={} 账户余额={}",
                                u.getId(), u.getUsername(), moneySum, money);
                    }
                }
                log.info("对账完成: 用户{}人, 积分差异{}人, 资金差异{}人", users.size(), pointsDiff, moneyDiff);
            } catch (Exception e) {
                log.warn("对账任务异常(次日重试)", e);
            }
        });
    }
}

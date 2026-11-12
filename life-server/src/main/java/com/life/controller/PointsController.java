package com.life.controller;

import com.life.query.QueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifePointsTaskService;
import com.life.service.PointsService;
import com.life.service.RiskService;
import com.life.service.TaskCenterService;
import com.life.vo.PointsRankVO;
import com.life.vo.PointsSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 积分C端接口(均需登录, /points/** 已注册拦截器)
 *
 * 与既有 /points/record/*(本人流水分页) 平级共存无冲突;
 * 积分榜/任务列表等后续端点按需在本链路扩展(PointsRankVO 契约已保留)
 */
@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;
    private final LifePointsTaskService lifePointsTaskService;
    private final TaskCenterService taskCenterService;
    private final RiskService riskService;

    /**
     * 积分总览: 余额 + 本月获得 + 本月消耗(前端 api/user.js getPointsSummary 已切至本接口)
     */
    @GetMapping("/summary")
    public Result<PointsSummaryVO> summary() {
        return Result.success(pointsService.querySummary());
    }

    /**
     * 本月积分榜(口径 = 本月获得积分, 查询即最新, 无需每日刷新; 自然月切换自动重置):
     * GET /points/rank?pageNum=1&pageSize=3 — 前端首页前三名即 pageSize=3,
     * api/user.js getLeaderboard 目前为 mock, 联调后切至本接口
     */
    @GetMapping("/rank")
    public Result<List<PointsRankVO>> rank(QueryDTO query) {
        return Result.success(pointsService.queryRankBoard(query));
    }

    /**
     * 我的本月名次与本月获得积分
     */
    @GetMapping("/rank/me")
    public Result<PointsRankVO> myRank() {
        return Result.success(pointsService.queryMyRank());
    }

    /**
     * 积分任务 C端列表(1.6+2.4): GET /points/task/list — 上架任务分页 + 本人进度
     * (todayCompleted 今日完成次数 / claimable 待领取次数, 由 TaskCenterService 批量回填)
     */
    @GetMapping("/task/list")
    public Result<PageResult> taskList(QueryDTO query) {
        return Result.success(lifePointsTaskService.queryCEndPage(query));
    }

    /**
     * 2.4 领取任务奖励: POST /points/task/{id}/claim —
     * 取最早一条"已完成待领取"记录 CAS 置已领取(并发双击只成功一次),
     * 积分经 MQ 异步发放(bizNo=TASKCLAIM:{recordId} 幂等, broker故障降级同步), 返回本次积分数
     */
    @PostMapping("/task/{id}/claim")
    public Result<Integer> claimTask(@PathVariable Long id) {
        Long userId = com.life.context.BaseContext.getCurrentId();
        // 2.10 风控闸门: 被标记风险的用户暂停积分领取
        riskService.checkAndCollect(com.life.constant.RiskActionConstant.CLAIM, userId);
        return Result.success(taskCenterService.claim(userId, id));
    }
}

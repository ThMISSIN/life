package com.life.controller;

import com.life.query.QueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifePointsTaskService;
import com.life.service.PointsService;
import com.life.vo.PointsRankVO;
import com.life.vo.PointsSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
     * 积分任务 C端只读列表(1.6): GET /points/task/list — 上架任务分页(前端替换 mock 用);
     * 领取奖励属二期(路线图 2.4)
     */
    @GetMapping("/task/list")
    public Result<PageResult> taskList(QueryDTO query) {
        return Result.success(lifePointsTaskService.queryCEndPage(query));
    }
}

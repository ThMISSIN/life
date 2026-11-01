package com.life.service;

import com.life.query.QueryDTO;
import com.life.vo.PointsRankVO;
import com.life.vo.PointsSummaryVO;

import java.util.List;

/**
 * 积分C端服务(均需登录) — 积分域统一入口
 * (roadmap 2.2 的 changePoints(userId, points, type, bizNo, remark) 公共加减分重构落地后,
 *  签到/下单/作废等散落的积分变更也应收进本服务统一走)
 */
public interface PointsService {

    /**
     * 积分总览: 当前余额 + 本月获得 + 本月消耗
     */
    PointsSummaryVO querySummary();

    /**
     * 本月积分榜: 口径 = 本月正数流水求和(不是 life_user.points 余额, 余额会被消费扣减干扰且霸榜固化);
     * 实时聚合查询即最新, 无需每日刷新任务; 自然月切换榜单自动重置, 无历史月榜
     *
     * @param query pageNum/pageSize, pageSize 即取前 N 名(前端首页默认 3)
     */
    List<PointsRankVO> queryRankBoard(QueryDTO query);

    /**
     * 我的本月名次(名次 = 本月积分比我高的人数 + 1)与本月获得积分
     */
    PointsRankVO queryMyRank();

    /**
     * 月度榜单结算(定时任务入口): 结算上个自然月前三名, 发放奖励积分;
     * 自然月滚动即榜单重置, 无需清零动作
     */
    void settleMonthlyRank();
}

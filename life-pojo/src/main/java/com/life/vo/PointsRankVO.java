package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分排行榜条目VO — 对应前端 mock/user.js leaderboard 项的 rank/nickname/username/avatar/points;
 * mock 里的 verified/verifiedColor/pointsText 属装饰与格式化字段, 后端不提供, 由前端自行处理(12500 → "12.5k")
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsRankVO {

    /**
     * 名次, 从1开始
     */
    private Integer rank;

    /**
     * 用户ID(点击跳他人主页用)
     */
    private Long userId;

    /**
     * 昵称(为空时以 username 兜底, 口径同 queryAdminOrderPage)
     */
    private String nickname;

    /**
     * 用户名
     */
    private String username;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 积分
     */
    private Integer points;
}

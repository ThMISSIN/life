package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注操作结果VO(关注/取关统一返回)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowResultVO {

    /**
     * 被关注用户ID
     */
    private Long followUserId;

    /**
     * 操作后是否处于已关注状态
     */
    private Boolean followed;

    /**
     * 对方最新粉丝数
     */
    private Integer followerCount;

    /**
     * 当前用户最新关注数
     */
    private Integer followingCount;
}

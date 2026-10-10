package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 关注/粉丝列表项VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUserVO {

    /**
     * 用户信息
     */
    private AuthorVO author;

    /**
     * 建立关注关系的时间
     */
    private LocalDateTime followTime;
}

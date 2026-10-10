package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 帖子作者简略信息VO(社交场景通用: 帖子/评论/关注列表)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 当前登录用户是否已关注TA, 未登录时可为null
     */
    private Boolean followed;
}

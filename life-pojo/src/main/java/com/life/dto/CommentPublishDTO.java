package com.life.dto;

import lombok.Data;

/**
 * 发表评论DTO
 */
@Data
public class CommentPublishDTO {

    /**
     * 帖子ID, 必填
     */
    private Long postId;

    /**
     * 评论内容, 必填
     */
    private String content;

    /**
     * 父评论ID, 0或不传表示顶级评论
     */
    private Long parentId;

    /**
     * 被回复用户ID, 回复二级评论时传
     */
    private Long replyUserId;
}

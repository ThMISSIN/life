package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论VO(支持二级嵌套)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {

    private Long id;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 评论人信息
     */
    private AuthorVO author;

    /**
     * 父评论ID, 0为顶级评论
     */
    private Long parentId;

    /**
     * 被回复用户ID
     */
    private Long replyUserId;

    /**
     * 被回复用户昵称, 渲染"回复 @xxx"
     */
    private String replyNickname;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 当前登录用户是否已点赞
     */
    private Boolean liked;

    /**
     * 子评论列表, 仅详情/首屏加载时填充
     */
    private List<CommentVO> children;

    /**
     * 评论时间
     */
    private LocalDateTime createTime;
}

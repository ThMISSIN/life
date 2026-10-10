package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 评论表
 */
@Data
@TableName("life_comment")
public class LifeComment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 父评论ID, 0表示顶级评论
     */
    private Long parentId;

    /**
     * 被回复用户ID
     */
    private Long replyUserId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 状态: 0-待审核, 1-正常, 2-已删除
     */
    private Integer status;
}

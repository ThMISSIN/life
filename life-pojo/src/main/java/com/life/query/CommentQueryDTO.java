package com.life.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommentQueryDTO extends QueryDTO {

    /**
     * 帖子ID, 必填
     */
    private Long postId;

    private Long parentId;
}

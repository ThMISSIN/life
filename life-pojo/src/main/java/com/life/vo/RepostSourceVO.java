package com.life.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 转发源帖摘要(嵌套在 PostVO.repostSource, 一期只带展示所需最小字段)
 */
@Data
@Builder
public class RepostSourceVO implements Serializable {

    private Long id;

    /**
     * 源帖作者昵称(空则username兜底; 源作者查不到时为"用户已注销")
     */
    private String authorName;

    /**
     * 源帖正文(超140字符截断)
     */
    private String content;

    /**
     * 源帖首图(可空)
     */
    private String image;

    private LocalDateTime createTime;

    /**
     * 源帖是否仍可见(已删/查不到时 false, 前端置灰"原帖已删除")
     */
    private Boolean available;
}

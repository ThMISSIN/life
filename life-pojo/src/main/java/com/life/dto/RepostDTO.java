package com.life.dto;

import lombok.Data;

/**
 * 帖子转发入参(POST /post/{id}/repost) — 一期仅正文附言, 转发不可带图/改分类
 */
@Data
public class RepostDTO {

    /**
     * 转发附言(可空, 空则生成纯转发帖, 前端展示源帖摘要)
     */
    private String content;
}

package com.life.dto;

import lombok.Data;

import java.util.List;

/**
 * 发布帖子DTO
 */
@Data
public class PostPublishDTO {

    /**
     * 帖子ID, 可空(新增时为null, 修改时为帖子ID)
     */
    private Long id;

    /**
     * 帖子标题, 可空(前端纯文本流)
     */
    private String title;

    /**
     * 帖子内容, 必填
     */
    private String content;

    /**
     * 图片URL列表, 服务端序列化为JSON数组存库
     */
    private List<String> images;

    /**
     * 帖子分类标签, 可空
     */
    private String category;
}

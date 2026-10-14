package com.life.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostQueryDTO extends QueryDTO {

    /**
     * 信息流类型: 1-为你推荐(默认), 2-正在关注
     */
    private Integer type;

    /**
     * 作者用户ID, 查看指定用户动态时传
     */
    private Long userId;

    /**
     * 分类标签
     */
    private String category;

    /**
     * 关键词, 按标题/内容模糊搜索
     */
    private String keyword;

    /**
     * 当前登录用户ID: 服务层强制覆盖(关注流子查询用), 不信任前端传值
     */
    private Long currentUserId;
}

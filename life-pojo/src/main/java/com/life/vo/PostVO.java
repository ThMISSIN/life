package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子列表/信息流VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostVO {

    private Long id;

    /**
     * 作者信息
     */
    private AuthorVO author;

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 图片URL列表(库中JSON数组, 出参解析为List)
     */
    private List<String> images;

    /**
     * 帖子分类标签
     */
    private String category;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 转发数, 表暂无该列, 一期固定返回0(前端卡片已预留reposts位)
     */
    private Integer repostCount;

    /**
     * 当前登录用户是否已点赞, 未登录时可为null
     */
    private Boolean liked;

    /**
     * 状态: 0-草稿, 1-已发布, 2-置顶, 3-隐藏, 4-已删除
     */
    private Integer status;

    /**
     * 发布时间
     */
    private LocalDateTime createTime;
}

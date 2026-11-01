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
     * 转发数(life_post.repost_count 实列, 3.4 一期)
     */
    private Integer repostCount;

    /**
     * 转发源帖子ID(原创帖为null)
     */
    private Long repostSourceId;

    /**
     * 转发源帖摘要(列表组装; 源帖已删时 available=false 占位)
     */
    private RepostSourceVO repostSource;

    /**
     * 当前登录用户是否已点赞, 未登录时可为null
     */
    private Boolean liked;

    /**
     * 当前登录用户是否已收藏(信息流批量回填, 参照 liked)
     */
    private Boolean favorited;

    /**
     * 状态: 0-草稿, 1-已发布, 2-置顶, 3-隐藏, 4-已删除
     */
    private Integer status;

    /**
     * 发布时间
     */
    private LocalDateTime createTime;
}

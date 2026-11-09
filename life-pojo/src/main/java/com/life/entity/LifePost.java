package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 帖子表
 */
@Data
@TableName("life_post")
public class LifePost extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 图片(JSON数组)
     */
    private String images;

    /**
     * 帖子分类
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
     * 转发数(repost_post 生成时原子 +1, 源帖删除时 -1)
     */
    private Integer repostCount;

    /**
     * 转发源帖子ID, 原创帖为NULL
     */
    private Long repostSourceId;

    /**
     * 状态: 0-草稿, 1-已发布, 2-置顶, 3-隐藏, 4-已删除
     */
    private Integer status;

    /**
     * 热度分(2.11): 基础分+互动权重(赞/评/转/阅)+新帖扶持-时间衰减;
     * 发帖初始化、互动回写增量、衰减扫描任务维护, 信息流按此排序
     */
    private BigDecimal hotScore;
}

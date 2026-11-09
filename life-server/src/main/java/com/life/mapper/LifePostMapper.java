package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifePost;
import com.life.properties.PostHotProperties;
import com.life.query.PostQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

@Mapper
public interface LifePostMapper extends BaseMapper<LifePost> {

    /**
     * 回写点赞总数: 值来自Redis点赞SET的size(终值), 整写覆盖天然幂等, 重复消费不漂移
     */
    @Update("UPDATE life_post SET like_count = #{likedTimes} WHERE id = #{bizId}")
    int updateLikeCount(@Param("bizId") Long bizId, @Param("likedTimes") Integer likedTimes);

    /**
     * 帖子分页列表(信息流/用户动态), SQL在LifePostMapper.xml
     */
    Page<LifePost> queryPostPage(Page<LifePost> page, @Param("query") PostQueryDTO query);

    /**
     * 转发数原子+1(仅未删行; 与转发帖生成同事务, 并发转发不丢计数)
     */
    @Update("UPDATE life_post SET repost_count = repost_count + 1 WHERE id = #{postId} AND deleted = 0")
    int incRepostCount(@Param("postId") Long postId);

    /**
     * 转发数原子-1(删除转发帖时回冲源帖; GREATEST 防计数为负)
     */
    @Update("UPDATE life_post SET repost_count = GREATEST(repost_count - 1, 0) WHERE id = #{postId} AND deleted = 0")
    int decRepostCount(@Param("postId") Long postId);

    /**
     * 评论数增量维护(2.11 前置): addComment +1 / deleteComment -(1+子评论数);
     * GREATEST 防计数为负, 存量欠账由 sql/07 一次性补账
     */
    @Update("UPDATE life_post SET comment_count = GREATEST(comment_count + #{delta}, 0) " +
            "WHERE id = #{postId} AND deleted = 0")
    int incCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

    /**
     * 浏览量增量回写(2.11): 阅读上报先进Redis ZSet累计, 任务排空后原子累加DB
     */
    @Update("UPDATE life_post SET view_count = view_count + #{delta} WHERE id = #{postId} AND deleted = 0")
    int incViewCount(@Param("postId") Long postId, @Param("delta") int delta);

    /**
     * 热度重算-单帖(2.11): 发帖初始化/点赞回写/评论增删/转发/阅读回写后调用,
     * 全部计数取行内现值, 单条UPDATE原子重算, 与批量扫描同公式(LifePostMapper.xml hotScoreExpr)
     */
    int recalcHotScore(@Param("postId") Long postId, @Param("h") PostHotProperties.Hot hot);

    /**
     * 热度重算-按id集合(点赞回写任务搭车: like_count 刚写回DB, 顺路刷新热度)
     */
    int recalcHotScoreByIds(@Param("ids") Collection<Long> ids, @Param("h") PostHotProperties.Hot hot);

    /**
     * 热度重算-衰减扫描: 重算发布在窗口内的已发布帖(单轮上限 recalcBatchSize),
     * 使时间衰减持续生效、24h新帖扶持到期自动摘除, 老帖无需扫描(扶持已在窗口内摘除)
     */
    int recalcHotScoreRecent(@Param("h") PostHotProperties.Hot hot);

    /**
     * 布隆过滤器存量重建: 按id游标分页扫全部帖子id(含已删——布隆无需精确, 少灌只会多拦)
     */
    @Select("SELECT id FROM life_post WHERE id > #{lastId} ORDER BY id LIMIT #{size}")
    List<Long> selectIdsAfter(@Param("lastId") Long lastId, @Param("size") int size);
}

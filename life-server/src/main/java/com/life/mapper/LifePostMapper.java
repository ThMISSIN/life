package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifePost;
import com.life.query.PostQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
}

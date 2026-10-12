package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.life.entity.LifePost;
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
}

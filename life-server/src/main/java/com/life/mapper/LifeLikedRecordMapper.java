package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.life.entity.LifeLikedRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LifeLikedRecordMapper extends BaseMapper<LifeLikedRecord> {

    /**
     * 按业务批量物理删除:
     * 定时任务用Redis快照全量重建这些biz的点赞行, 不能用MP的remove(逻辑删除),
     * 否则尸体行占住uk_user_biz让saveBatch撞键
     */
    int deleteByBiz(@Param("bizType") String bizType, @Param("bizIds") List<Long> bizIds);
}

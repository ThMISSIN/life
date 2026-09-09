package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifePointsRecord;
import com.life.query.QueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LifePointsRecordMapper extends BaseMapper<LifePointsRecord> {

    Page<LifePointsRecord> queryList(Page<LifePointsRecord> page,
                                     @Param("userId") Long userId,
                                     @Param("queryDTO") QueryDTO queryDTO);
}

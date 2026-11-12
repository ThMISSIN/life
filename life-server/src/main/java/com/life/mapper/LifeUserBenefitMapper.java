package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.life.entity.LifeUserBenefit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LifeUserBenefitMapper extends BaseMapper<LifeUserBenefit> {

    /**
     * 权益过期落库(5.2/checklist 二.2): UNUSED(1) 且 expire_time 已到 → EXPIRED(3),
     * 此前"已过期"仅查询时实时推导, 报表/对账口径不一致; 落库后展示层修正逻辑保留兼容
     */
    @Update("UPDATE life_user_benefit SET status = 3, update_time = NOW() " +
            "WHERE status = 1 AND expire_time IS NOT NULL AND expire_time < NOW() AND deleted = 0")
    int markExpired();
}

package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifeBenefit;
import com.life.query.BenefitQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LifeBenefitMapper extends BaseMapper<LifeBenefit> {
    Page<LifeBenefit> queryList(Page<LifeBenefit> page, @Param("query") BenefitQueryDTO query);

    @Update("update life_social.life_benefit set stock = stock - 1 where id = #{id} and stock > 0 and sales = sales + 1")
    int deductStock(Long id);
}

package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifeProduct;
import com.life.query.ProductQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LifeProductMapper extends BaseMapper<LifeProduct> {

    /**
     * 商品分页列表(动态SQL在LifeProductMapper.xml)
     * status由服务层控制: C端强制1-上架, 管理端可传null查全部
     */
    Page<LifeProduct> queryList(Page<LifeProduct> page, @Param("query") ProductQueryDTO query);
}

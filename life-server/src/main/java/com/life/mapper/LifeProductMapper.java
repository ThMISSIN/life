package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifeProduct;
import com.life.query.ProductQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LifeProductMapper extends BaseMapper<LifeProduct> {

    /**
     * 商品分页列表(动态SQL在LifeProductMapper.xml)
     * status由服务层控制: C端强制1-上架, 管理端可传null查全部
     */
    Page<LifeProduct> queryList(Page<LifeProduct> page, @Param("query") ProductQueryDTO query);

    /**
     * 扣库存(乐观锁防超卖): 条件 stock >= quantity 与更新同行原子判定, 0行=库存被并发抢完
     * -1不限量: 条件恒过且库存保持-1, 只加销量
     */
    @Update("update life_product set stock = if(stock = -1, -1, stock - #{quantity}), sales = sales + #{quantity} " +
            "where id = #{itemId} and (stock = -1 or stock >= #{quantity})")
    int deductStock(Long itemId, Integer quantity);

    /**
     * 退库存(取消/关单回滚): -1不限量保持-1, 同步回退销量(防负)
     */
    @Update("UPDATE life_product SET stock = if(stock = -1, -1, stock + #{quantity}), sales = greatest(sales - #{quantity}, 0) " +
            "WHERE id = #{itemId}")
    void backStock(Long itemId, Integer quantity);
}

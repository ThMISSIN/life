package com.ruoyi.life.mapper;

import com.ruoyi.life.domain.LifeProduct;
import com.ruoyi.life.domain.query.LifeProductQuery;

import java.util.List;

/**
 * 商品 数据层
 */
public interface LifeProductMapper
{
    /** 管理端分页查询(status不传查全部, 含下架) */
    public List<LifeProduct> selectLifeProductList(LifeProductQuery query);

    /** 按ID查询 */
    public LifeProduct selectLifeProductById(Long id);

    /** 新增 */
    public int insertLifeProduct(LifeProduct lifeProduct);

    /** 修改 */
    public int updateLifeProduct(LifeProduct lifeProduct);

    /** 逻辑删除 */
    public int deleteLifeProductById(Long id);

    /** 分类下商品数(删除分类前校验) */
    public int countByCategoryId(Long categoryId);
}

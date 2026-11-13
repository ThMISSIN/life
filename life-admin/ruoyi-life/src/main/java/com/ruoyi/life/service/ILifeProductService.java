package com.ruoyi.life.service;

import com.ruoyi.life.domain.LifeProduct;
import com.ruoyi.life.domain.query.LifeProductQuery;

import java.util.List;

/**
 * 商品管理 服务接口（迁移自C端 LifeProductService 管理端方法）
 */
public interface ILifeProductService
{
    /** 分页查询(status不传查全部, 含下架); 分页由Controller startPage()接管 */
    public List<LifeProduct> selectLifeProductList(LifeProductQuery query);

    /** 按ID查询 */
    public LifeProduct selectLifeProductById(Long id);

    /** 新增商品: 校验名称、分类、双通道价格 */
    public int insertLifeProduct(LifeProduct lifeProduct);

    /** 修改商品: 部分更新, 双通道校验按"传值覆盖原值"后的合并结果判断 */
    public int updateLifeProduct(LifeProduct lifeProduct);

    /** 上下架 */
    public int changeStatus(Long id, Integer status);

    /** 删除商品(软删除, 上架中禁止) */
    public int deleteLifeProductByIds(Long[] ids);
}

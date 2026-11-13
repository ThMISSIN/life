package com.ruoyi.life.service;

import com.ruoyi.life.domain.LifeProductCategory;

import java.util.List;

/**
 * 商品分类管理 服务接口（迁移自C端 LifeProductCategoryService 管理端方法）
 */
public interface ILifeProductCategoryService
{
    /** 全量分类列表(管理端含禁用) */
    public List<LifeProductCategory> selectLifeProductCategoryList();

    /** 按ID查询 */
    public LifeProductCategory selectLifeProductCategoryById(Long id);

    /** 新增分类: 校验名称、上级分类与层级一致性 */
    public int insertLifeProductCategory(LifeProductCategory lifeProductCategory);

    /** 修改分类: 名称与层级仅在传值时校验 */
    public int updateLifeProductCategory(LifeProductCategory lifeProductCategory);

    /** 删除分类: 存在子分类或挂载商品时禁止删除 */
    public int deleteLifeProductCategoryById(Long id);
}

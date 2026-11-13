package com.ruoyi.life.mapper;

import com.ruoyi.life.domain.LifeProductCategory;
import com.ruoyi.life.domain.query.LifeProductQuery;

import java.util.List;

/**
 * 商品分类 数据层
 */
public interface LifeProductCategoryMapper
{
    /** 查询分类列表(管理端含禁用) */
    public List<LifeProductCategory> selectLifeProductCategoryList(LifeProductQuery query);

    /** 按ID查询 */
    public LifeProductCategory selectLifeProductCategoryById(Long id);

    /** 按名称计数(重名校验, excludeId排除自身) */
    public int countByName(String name, Long excludeId);

    /** 子分类数(删除前校验) */
    public int countByParentId(Long parentId);

    /** 新增 */
    public int insertLifeProductCategory(LifeProductCategory lifeProductCategory);

    /** 修改 */
    public int updateLifeProductCategory(LifeProductCategory lifeProductCategory);

    /** 逻辑删除 */
    public int deleteLifeProductCategoryById(Long id);
}

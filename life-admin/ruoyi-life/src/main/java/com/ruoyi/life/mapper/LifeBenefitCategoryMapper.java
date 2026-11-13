package com.ruoyi.life.mapper;

import com.ruoyi.life.domain.LifeBenefitCategory;
import com.ruoyi.life.domain.query.LifeBenefitQuery;

import java.util.List;

/**
 * 权益分类 数据层
 */
public interface LifeBenefitCategoryMapper
{
    /** 查询分类列表(管理端含禁用) */
    public List<LifeBenefitCategory> selectLifeBenefitCategoryList(LifeBenefitQuery query);

    /** 按ID查询 */
    public LifeBenefitCategory selectLifeBenefitCategoryById(Long id);

    /** 按名称计数(重名校验, excludeId排除自身) */
    public int countByName(String name, Long excludeId);

    /** 子分类数(删除前校验) */
    public int countByParentId(Long parentId);

    /** 新增 */
    public int insertLifeBenefitCategory(LifeBenefitCategory lifeBenefitCategory);

    /** 修改 */
    public int updateLifeBenefitCategory(LifeBenefitCategory lifeBenefitCategory);

    /** 逻辑删除 */
    public int deleteLifeBenefitCategoryById(Long id);
}

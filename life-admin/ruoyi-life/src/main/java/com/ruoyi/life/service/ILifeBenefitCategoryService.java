package com.ruoyi.life.service;

import com.ruoyi.life.domain.LifeBenefitCategory;

import java.util.List;

/**
 * 权益分类管理 服务接口（迁移自C端 LifeBenefitCategoryService 管理端方法）
 */
public interface ILifeBenefitCategoryService
{
    /** 全量分类列表(管理端含禁用) */
    public List<LifeBenefitCategory> selectLifeBenefitCategoryList();

    /** 按ID查询 */
    public LifeBenefitCategory selectLifeBenefitCategoryById(Long id);

    /** 新增分类: 校验名称、上级分类与层级一致性 */
    public int insertLifeBenefitCategory(LifeBenefitCategory lifeBenefitCategory);

    /** 修改分类: 名称与层级仅在传值时校验 */
    public int updateLifeBenefitCategory(LifeBenefitCategory lifeBenefitCategory);

    /** 删除分类: 存在子分类或挂载权益时禁止删除 */
    public int deleteLifeBenefitCategoryById(Long id);
}

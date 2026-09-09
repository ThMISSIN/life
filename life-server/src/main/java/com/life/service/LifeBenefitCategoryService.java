package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.entity.LifeBenefitCategory;
import com.life.vo.LifeBenefitCategoryVO;

import java.util.List;

public interface LifeBenefitCategoryService extends IService<LifeBenefitCategory> {

    /**
     * C端查询分类树(仅启用状态的分类)
     */
    List<LifeBenefitCategoryVO> getCategoryTree();

    /**
     * 管理端新增分类
     */
    void addCategory(LifeBenefitCategory lifeBenefitCategory);

    /**
     * 管理端修改分类
     */
    void updateCategory(LifeBenefitCategory lifeBenefitCategory);

    /**
     * 管理端删除分类(软删除)
     */
    void removeCategory(Long id);
}

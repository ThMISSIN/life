package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.entity.LifeProductCategory;
import com.life.vo.LifeProductCategoryVO;

import java.util.List;

public interface LifeProductCategoryService extends IService<LifeProductCategory> {

    /**
     * C端查询商品分类树
     */
    List<LifeProductCategoryVO> getCategoryTree();

    /**
     * 管理端新增分类
     */
    void addCategory(LifeProductCategory lifeProductCategory);

    /**
     * 管理端修改分类
     */
    void updateCategory(LifeProductCategory lifeProductCategory);

    /**
     * 管理端删除分类(存在子分类或已挂载商品时禁止)
     */
    void removeCategory(Long id);
}

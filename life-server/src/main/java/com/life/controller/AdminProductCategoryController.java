package com.life.controller;

import com.life.entity.LifeProductCategory;
import com.life.result.Result;
import com.life.service.LifeProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品分类管理端接口
 */
@RestController
@RequestMapping("/admin/product/category")
public class AdminProductCategoryController {

    @Autowired
    private LifeProductCategoryService lifeProductCategoryService;

    /**
     * 新增商品分类
     */
    @PostMapping
    public Result add(@RequestBody LifeProductCategory lifeProductCategory) {
        lifeProductCategoryService.addCategory(lifeProductCategory);
        return Result.success();
    }

    /**
     * 修改商品分类
     */
    @PutMapping
    public Result update(@RequestBody LifeProductCategory lifeProductCategory) {
        lifeProductCategoryService.updateCategory(lifeProductCategory);
        return Result.success();
    }

    /**
     * 删除商品分类(软删除)
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        lifeProductCategoryService.removeCategory(id);
        return Result.success();
    }
}

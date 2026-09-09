package com.life.controller;

import com.life.entity.LifeBenefitCategory;
import com.life.result.Result;
import com.life.service.LifeBenefitCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权益分类管理端接口
 */
@RestController
@RequestMapping("/admin/benefit/category")
public class AdminBenefitCategoryController {

    @Autowired
    private LifeBenefitCategoryService lifeBenefitCategoryService;

    /**
     * 新增权益分类
     */
    @PostMapping
    public Result add(@RequestBody LifeBenefitCategory lifeBenefitCategory) {
        lifeBenefitCategoryService.addCategory(lifeBenefitCategory);
        return Result.success();
    }

    /**
     * 删除权益分类(软删除)
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        lifeBenefitCategoryService.removeCategory(id);
        return Result.success();
    }

    /**
     * 修改权益分类
     */
    @PutMapping
    public Result update(@RequestBody LifeBenefitCategory lifeBenefitCategory) {
        lifeBenefitCategoryService.updateCategory(lifeBenefitCategory);
        return Result.success();
    }
}

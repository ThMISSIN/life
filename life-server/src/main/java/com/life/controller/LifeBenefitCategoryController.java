package com.life.controller;

import com.life.result.Result;
import com.life.service.LifeBenefitCategoryService;
import com.life.vo.LifeBenefitCategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权益分类 C端接口
 */
@RestController
@RequestMapping("/benefit/category")
public class LifeBenefitCategoryController {

    @Autowired
    private LifeBenefitCategoryService lifeBenefitCategoryService;

    /**
     * 查询权益分类树
     */
    @GetMapping
    public Result<List<LifeBenefitCategoryVO>> getCategoryTree() {
        List<LifeBenefitCategoryVO> vos = lifeBenefitCategoryService.getCategoryTree();
        return Result.success(vos);
    }
}

package com.life.controller;

import com.life.result.Result;
import com.life.service.LifeProductCategoryService;
import com.life.vo.LifeProductCategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类 C端接口
 */
@RestController
@RequestMapping("/product/category")
public class LifeProductCategoryController {

    @Autowired
    private LifeProductCategoryService lifeProductCategoryService;

    /**
     * 查询商品分类树
     */
    @GetMapping
    public Result<List<LifeProductCategoryVO>> getCategoryTree() {
        List<LifeProductCategoryVO> vos = lifeProductCategoryService.getCategoryTree();
        return Result.success(vos);
    }
}

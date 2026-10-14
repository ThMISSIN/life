package com.life.controller;

import com.life.query.ProductQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifeProductService;
import com.life.vo.ProductDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品 C端接口: 浏览/详情
 */
@RestController
@RequestMapping("/product")
public class LifeProductController {

    @Autowired
    private LifeProductService lifeProductService;

    /**
     * 分页查询已上架商品(分类/关键词/排序)
     */
    @GetMapping("/page")
    public Result<PageResult> page(ProductQueryDTO query) {
        PageResult pageResult = lifeProductService.pageList(query);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductDetailVO> getById(@PathVariable Long id) {
        ProductDetailVO detail = lifeProductService.getProductDetail(id);
        return Result.success(detail);
    }
}

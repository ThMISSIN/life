package com.life.controller;

import com.life.entity.LifeProduct;
import com.life.query.ProductQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifeProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 商品管理端接口
 */
@RestController
@RequestMapping("/admin/product")
public class AdminProductController {

    @Autowired
    private LifeProductService lifeProductService;

    /**
     * 分页查询商品(可见下架数据, status不传查全部)
     */
    @GetMapping("/page")
    public Result<PageResult> page(ProductQueryDTO query) {
        PageResult pageResult = lifeProductService.adminPageList(query);
        return Result.success(pageResult);
    }

    /**
     * 新增商品
     */
    @PostMapping
    public Result add(@RequestBody LifeProduct lifeProduct) {
        lifeProductService.addProduct(lifeProduct);
        return Result.success();
    }

    /**
     * 修改商品
     */
    @PutMapping
    public Result update(@RequestBody LifeProduct lifeProduct) {
        lifeProductService.updateProduct(lifeProduct);
        return Result.success();
    }

    /**
     * 上下架
     */
    @PutMapping("/{id}/status/{status}")
    public Result updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        lifeProductService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 删除商品(软删除, 上架中禁止)
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        lifeProductService.removeProduct(id);
        return Result.success();
    }
}

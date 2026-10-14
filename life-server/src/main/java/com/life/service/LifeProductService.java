package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.entity.LifeProduct;
import com.life.query.ProductQueryDTO;
import com.life.result.PageResult;
import com.life.vo.ProductDetailVO;

public interface LifeProductService extends IService<LifeProduct> {

    /**
     * C端分页查询已上架商品
     */
    PageResult pageList(ProductQueryDTO query);

    /**
     * 管理端分页查询(可见下架数据)
     */
    PageResult adminPageList(ProductQueryDTO query);

    /**
     * C端查询商品详情(仅上架商品)
     */
    ProductDetailVO getProductDetail(Long id);

    /**
     * 管理端新增商品
     */
    void addProduct(LifeProduct lifeProduct);

    /**
     * 管理端修改商品
     */
    void updateProduct(LifeProduct lifeProduct);

    /**
     * 管理端删除商品(软删除, 上架中禁止删除)
     */
    void removeProduct(Long id);

    /**
     * 管理端上下架
     */
    void updateStatus(Long id, Integer status);
}

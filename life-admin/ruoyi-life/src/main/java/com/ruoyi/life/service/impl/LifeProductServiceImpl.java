package com.ruoyi.life.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.life.constant.LifeConstants;
import com.ruoyi.life.domain.LifeProduct;
import com.ruoyi.life.domain.query.LifeProductQuery;
import com.ruoyi.life.mapper.LifeProductCategoryMapper;
import com.ruoyi.life.mapper.LifeProductMapper;
import com.ruoyi.life.service.ILifeProductService;
import com.ruoyi.life.util.LifeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品管理 服务实现（业务口径与C端 LifeProductServiceImpl 管理端方法一致）
 *
 * @author life
 */
@Service
public class LifeProductServiceImpl implements ILifeProductService
{
    @Autowired
    private LifeProductMapper lifeProductMapper;

    @Autowired
    private LifeProductCategoryMapper lifeProductCategoryMapper;

    @Override
    public List<LifeProduct> selectLifeProductList(LifeProductQuery query)
    {
        // status为null查全部(含下架), 由管理端自行筛选
        return lifeProductMapper.selectLifeProductList(query);
    }

    @Override
    public LifeProduct selectLifeProductById(Long id)
    {
        return lifeProductMapper.selectLifeProductById(id);
    }

    /**
     * 管理端新增商品: 校验名称、分类、双通道价格
     */
    @Override
    public int insertLifeProduct(LifeProduct lifeProduct)
    {
        if (lifeProduct.getName() == null || lifeProduct.getName().isBlank())
        {
            throw new ServiceException("商品名称不能为空");
        }
        if (lifeProduct.getCategoryId() == null)
        {
            throw new ServiceException("商品分类不能为空");
        }
        checkCategoryExists(lifeProduct.getCategoryId());
        checkPriceChannel(lifeProduct.getPrice(), lifeProduct.getPointsPrice());
        lifeProduct.setCreateTime(LifeUtils.now());
        lifeProduct.setUpdateTime(LifeUtils.now());
        lifeProduct.setCreater(LifeUtils.currentOperator());
        lifeProduct.setUpdater(LifeUtils.currentOperator());
        lifeProduct.setDeleted(0);
        return lifeProductMapper.insertLifeProduct(lifeProduct);
    }

    /**
     * 管理端修改商品: 部分更新, 双通道校验按"传值覆盖原值"后的合并结果判断
     */
    @Override
    public int updateLifeProduct(LifeProduct lifeProduct)
    {
        if (lifeProduct.getId() == null)
        {
            throw new ServiceException("商品ID不能为空");
        }
        LifeProduct existing = lifeProductMapper.selectLifeProductById(lifeProduct.getId());
        if (existing == null)
        {
            throw new ServiceException("商品不存在");
        }
        if (lifeProduct.getName() != null && lifeProduct.getName().isBlank())
        {
            throw new ServiceException("商品名称不能为空");
        }
        if (lifeProduct.getCategoryId() != null)
        {
            checkCategoryExists(lifeProduct.getCategoryId());
        }
        int price = lifeProduct.getPrice() != null ? lifeProduct.getPrice() : nvl(existing.getPrice());
        int pointsPrice = lifeProduct.getPointsPrice() != null ? lifeProduct.getPointsPrice() : nvl(existing.getPointsPrice());
        checkPriceChannel(price, pointsPrice);
        lifeProduct.setUpdateTime(LifeUtils.now());
        lifeProduct.setUpdater(LifeUtils.currentOperator());
        return lifeProductMapper.updateLifeProduct(lifeProduct);
    }

    /**
     * 管理端上下架
     */
    @Override
    public int changeStatus(Long id, Integer status)
    {
        if (status == null || (status != LifeConstants.LISTED && status != LifeConstants.UNLISTED))
        {
            throw new ServiceException("商品状态仅支持: 0-下架, 1-上架");
        }
        LifeProduct product = lifeProductMapper.selectLifeProductById(id);
        if (product == null)
        {
            throw new ServiceException("商品不存在");
        }
        LifeProduct update = new LifeProduct();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(LifeUtils.now());
        update.setUpdater(LifeUtils.currentOperator());
        return lifeProductMapper.updateLifeProduct(update);
    }

    /**
     * 管理端删除商品: 上架中禁止删除, 避免用户正在浏览/下单的商品凭空消失
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteLifeProductByIds(Long[] ids)
    {
        int count = 0;
        for (Long id : ids)
        {
            LifeProduct product = lifeProductMapper.selectLifeProductById(id);
            if (product == null)
            {
                throw new ServiceException("商品不存在");
            }
            if (product.getStatus() != null && product.getStatus() == LifeConstants.LISTED)
            {
                throw new ServiceException("商品[" + product.getName() + "]上架中, 禁止删除");
            }
            count += lifeProductMapper.deleteLifeProductById(id);
        }
        return count;
    }

    /**
     * 双通道校验: price=0不可现金购买, points_price=0不可积分兑换, 两条通道不能同时关闭
     */
    private void checkPriceChannel(Integer price, Integer pointsPrice)
    {
        int p = nvl(price);
        int pp = nvl(pointsPrice);
        if (p == LifeConstants.NO_CASH_PRICE && pp == LifeConstants.NO_POINTS_PRICE)
        {
            throw new ServiceException("现金价格与积分价格不能同时为0, 至少开启一条购买通道");
        }
    }

    private void checkCategoryExists(Long categoryId)
    {
        if (lifeProductCategoryMapper.selectLifeProductCategoryById(categoryId) == null)
        {
            throw new ServiceException("商品分类不存在");
        }
    }

    private int nvl(Integer value)
    {
        return value == null ? 0 : value;
    }
}

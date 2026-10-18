package com.life.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.MessageConstant;
import com.life.constant.ProductConstant;
import com.life.entity.LifeProduct;
import com.life.entity.LifeUser;
import com.life.exception.ValidationException;
import com.life.mapper.LifeOrderDetailMapper;
import com.life.mapper.LifeProductMapper;
import com.life.query.ProductQueryDTO;
import com.life.result.PageResult;
import com.life.service.LifeProductService;
import com.life.vo.ProductDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class LifeProductServiceImpl extends ServiceImpl<LifeProductMapper, LifeProduct> implements LifeProductService {

    @Autowired
    private LifeOrderDetailMapper orderDetailMapper;

    @Override
    public PageResult pageList(ProductQueryDTO query) {
        // C端无论前端传什么, 强制只看上架数据
        query.setStatus(ProductConstant.LISTED);
        return doPage(query);
    }

    @Override
    public PageResult adminPageList(ProductQueryDTO query) {
        // status为null查全部(含下架), 由管理端自行筛选
        return doPage(query);
    }

    private PageResult doPage(ProductQueryDTO query) {
        Page<LifeProduct> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<LifeProduct> resultPage = baseMapper.queryList(page, query);
        return new PageResult(resultPage.getTotal(), resultPage.getRecords());
    }

    /**
     * C端查询商品详情: 仅返回已上架商品, 附带库存是否充足标识
     */
    @Override
    public ProductDetailVO getProductDetail(Long id) {
        LifeProduct product = getById(id);
        // 过滤不存在 以及 未上架 商品
        if (product == null || product.getStatus() == null || product.getStatus() != ProductConstant.LISTED) {
            throw new ValidationException(MessageConstant.PRODUCT_NOT_FOUND);
        }

        return ProductDetailVO.builder()
                .id(product.getId())
                .name(product.getName())
                .categoryId(product.getCategoryId())
                .description(product.getDescription())
                .coverImage(product.getCoverImage())
                .images(parseImages(product.getImages()))
                .tags(parseTags(product.getTags()))
                .shopName(product.getShopName())
                .location(product.getLocation())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .pointsPrice(product.getPointsPrice())
                .perUserLimit(product.getPerUserLimit())
                .minLevel(product.getMinLevel())
                .stock(product.getStock())
                .sales(product.getSales())
                // 0-无货; -1不限量与正数库存均视为有货(下单时原子扣减兜底)
                .inStock(product.getStock() != null && product.getStock() != ProductConstant.NO_STOCK)
                .build();
    }

    /**
     * 管理端新增商品: 校验名称、分类、双通道价格
     */
    @Override
    public void addProduct(LifeProduct lifeProduct) {
        if (lifeProduct.getName() == null || lifeProduct.getName().isBlank()) {
            throw new ValidationException(MessageConstant.PRODUCT_NAME_REQUIRED);
        }
        if (lifeProduct.getCategoryId() == null) {
            throw new ValidationException(MessageConstant.PRODUCT_CATEGORY_ID_REQUIRED);
        }
        checkPriceChannel(nullToZero(lifeProduct.getPrice()), nullToZero(lifeProduct.getPointsPrice()));
        save(lifeProduct);
    }

    /**
     * 管理端修改商品: 部分更新, 双通道校验按"传值覆盖原值"后的合并结果判断
     */
    @Override
    public void updateProduct(LifeProduct lifeProduct) {
        if (lifeProduct.getId() == null) {
            throw new ValidationException(MessageConstant.PRODUCT_ID_REQUIRED);
        }
        LifeProduct existing = getById(lifeProduct.getId());
        if (existing == null) {
            throw new ValidationException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (lifeProduct.getName() != null && lifeProduct.getName().isBlank()) {
            throw new ValidationException(MessageConstant.PRODUCT_NAME_REQUIRED);
        }
        int price = lifeProduct.getPrice() != null ? lifeProduct.getPrice() : nullToZero(existing.getPrice());
        int pointsPrice = lifeProduct.getPointsPrice() != null ? lifeProduct.getPointsPrice() : nullToZero(existing.getPointsPrice());
        checkPriceChannel(price, pointsPrice);
        updateById(lifeProduct);
    }

    /**
     * 管理端删除商品: 上架中禁止删除, 避免用户正在浏览/下单的商品凭空消失
     */
    @Override
    public void removeProduct(Long id) {
        LifeProduct product = getById(id);
        if (product == null) {
            throw new ValidationException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (product.getStatus() != null && product.getStatus() == ProductConstant.LISTED) {
            throw new ValidationException(MessageConstant.PRODUCT_ON_SALE);
        }
        removeById(id);
    }

    /**
     * 管理端上下架
     */
    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != ProductConstant.LISTED && status != ProductConstant.UNLISTED)) {
            throw new ValidationException("商品状态仅支持: 0-下架, 1-上架");
        }
        LifeProduct product = getById(id);
        if (product == null) {
            throw new ValidationException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        LifeProduct update = new LifeProduct();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
    }

    /**
     * 下单前置校验: 存在/上架/库存(-1为不限量)/等级/限购, 等级与限购口径与权益通道一致
     * todo 已知限制: 限购是查数判断, 并发双击可超限, 后续与限兑统一用Redis锁解决
     */
    @Override
    public LifeProduct validatePurchase(Long id, int quantity, LifeUser user) {
        LifeProduct product = getById(id);
        if (product == null || product.getStatus() == null || product.getStatus() != ProductConstant.LISTED) {
            throw new ValidationException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (product.getStock() != null && product.getStock() != -1 && product.getStock() < quantity) {
            throw new ValidationException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
        }
        if (product.getMinLevel() != null && product.getMinLevel() > 0 && user.getLevel() < product.getMinLevel()) {
            throw new ValidationException(MessageConstant.USER_LEVEL_NOT_ENOUGH);
        }
        // 限购: 已购数量(排除已取消/已退款) + 本单数量 不得超过上限
        if (product.getPerUserLimit() != null && product.getPerUserLimit() > 0
                && orderDetailMapper.countPurchased(user.getId(), id) + quantity > product.getPerUserLimit()) {
            throw new ValidationException(MessageConstant.PRODUCT_PURCHASE_LIMIT_REACHED);
        }
        return product;
    }

    /**
     * 双通道校验: price=0不可现金购买, points_price=0不可积分兑换, 两条通道不能同时关闭
     */
    private void checkPriceChannel(int price, int pointsPrice) {
        if (price == ProductConstant.NO_CASH_PRICE && pointsPrice == ProductConstant.NO_POINTS_PRICE) {
            throw new ValidationException(MessageConstant.PRODUCT_PRICE_CHANNEL_REQUIRED);
        }
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 详情图JSON数组字符串转列表, 数据异常时降级为空列表不影响展示
     */
    private List<String> parseImages(String images) {
        if (images == null || images.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return JSON.parseArray(images, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 标签逗号分隔串转列表
     */
    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}

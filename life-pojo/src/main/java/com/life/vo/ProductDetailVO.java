package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品详情VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailVO {

    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 封面图
     */
    private String coverImage;

    /**
     * 详情图列表
     */
    private List<String> images;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 发货地
     */
    private String location;

    /**
     * 现金价格(分), 0-不可现金购买
     */
    private Integer price;

    /**
     * 原价(分), 划线价
     */
    private Integer originalPrice;

    /**
     * 积分价格, 0-不可积分兑换
     */
    private Integer pointsPrice;

    /**
     * 每人限兑次数(积分通道), 0-不限
     */
    private Integer perUserLimit;

    /**
     * 积分兑换的最低会员等级, 0-不限
     */
    private Integer minLevel;

    /**
     * 库存, -1表示不限
     */
    private Integer stock;

    /**
     * 累计销量
     */
    private Integer sales;

    /**
     * 库存是否充足(前端按钮置灰依据)
     */
    private Boolean inStock;
}

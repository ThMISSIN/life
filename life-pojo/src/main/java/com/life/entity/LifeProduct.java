package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 商品表(双通道: 积分兑换+现金购买)
 */
@Data
@TableName("life_product")
public class LifeProduct extends BaseEntity {

    @TableId(type = IdType.AUTO)
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
     * 详情图(JSON数组)
     */
    private String images;

    /**
     * 标签(逗号分隔, 列表卡片展示)
     */
    private String tags;

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
     * 库存, -1表示不限量
     */
    private Integer stock;

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 状态: 0-下架, 1-上架
     */
    private Integer status;

    /**
     * 排序号
     */
    private Integer sort;
}

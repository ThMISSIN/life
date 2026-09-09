package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权益表
 */
@Data
@TableName("life_benefit")
public class LifeBenefit extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权益名称
     */
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 权益描述
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
     * 权益类型: 1-券码类, 2-直充类
     */
    private Integer benefitType;

    /**
     * 积分价格
     */
    private Integer pointsPrice;

    /**
     * 原价(分)
     */
    private Integer originalPrice;

    /**
     * 有效期类型: 1-相对天数, 2-固定截止日
     */
    private Integer validType;

    /**
     * 有效天数
     */
    private Integer validDays;

    /**
     * 固定截止时间
     */
    private LocalDateTime fixedEndTime;

    /**
     * 每人限兑次数, 0-不限
     */
    private Integer perUserLimit;

    /**
     * 可兑换的最低会员等级, 0-不限
     */
    private Integer minLevel;

    /**
     * 库存
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

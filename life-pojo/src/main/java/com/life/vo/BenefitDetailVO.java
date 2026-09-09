package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权益详情VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenefitDetailVO {

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
     * 详情图列表
     */
    private List<String> images;

    /**
     * 权益类型: 1-券码类, 2-直充类
     */
    private Integer benefitType;

    /**
     * 积分价格
     */
    private Integer pointsPrice;

    /**
     * 原价(分), 用于展示划线价
     */
    private Integer originalPrice;

    /**
     * 有效期类型: 1-相对天数, 2-固定截止日
     */
    private Integer validType;

    /**
     * 有效天数, valid_type=1时有值
     */
    private Integer validDays;

    /**
     * 固定截止时间, valid_type=2时有值
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
     * 库存, -1表示不限
     */
    private Integer stock;

    /**
     * 累计兑换量
     */
    private Integer sales;

    /**
     * 当前用户已兑换次数(不含已作废)
     */
    private Long myExchangedCount;

    /**
     * 当前用户是否可兑换(库存充足且未达限兑)
     */
    private Boolean canExchange;
}

package com.life.constant;

/**
 * 商品(life_product)相关状态与通道约定
 */
public class ProductConstant {

    /** 上架 */
    public static final int LISTED = 1;

    /** 下架 */
    public static final int UNLISTED = 0;

    /** 现金价格为0: 不可现金购买(纯积分商品) */
    public static final int NO_CASH_PRICE = 0;

    /** 积分价格为0: 不可积分兑换(纯现金商品) */
    public static final int NO_POINTS_PRICE = 0;

    /** 库存为0: 无货(-1表示不限量) */
    public static final int NO_STOCK = 0;
}

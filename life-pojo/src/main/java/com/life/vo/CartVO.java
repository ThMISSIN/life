package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 购物车行VO: 加购快照 + 商品实时状态(实时字段查询时组装, 不落库)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartVO {

    /**
     * 购物车行ID
     */
    private Long id;

    /**
     * 商品类型: 1-实物商品, 2-权益
     */
    private Integer itemType;

    /**
     * 商品ID/权益ID
     */
    private Long itemId;

    /**
     * 商品名称(加购时快照)
     */
    private String itemName;

    /**
     * 商品图片(加购时快照)
     */
    private String itemImage;

    /**
     * 现金单价(分, 加购时快照)
     */
    private Integer price;

    /**
     * 积分价格(加购时快照)
     */
    private Integer pointsPrice;

    /**
     * 数量
     */
    private Integer quantity;

    // ==== 以下为查询时组装的实时字段 ====

    /**
     * 实时库存(-1为不限量)
     */
    private Integer stock;

    /**
     * 当前是否上架
     */
    private Boolean onSale;

    /**
     * 有效标记(上架且有库存); 失效行前端置灰并禁止勾选结算
     */
    private Boolean valid;

    /**
     * 加购时间
     */
    private LocalDateTime createTime;
}

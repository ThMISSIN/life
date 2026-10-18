package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单明细VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailVO {

    private Long id;

    /**
     * 商品类型: 1-实物商品, 2-虚拟权益
     */
    private Integer itemType;

    /**
     * 商品ID/权益ID
     */
    private Long itemId;

    /**
     * 商品名称
     */
    private String itemName;

    /**
     * 商品图片
     */
    private String itemImage;

    /**
     * 单价(分)
     */
    private Integer price;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 总金额(分)
     */
    private Integer totalAmount;

    /**
     * 积分价格
     */
    private Integer pointsPrice;
}

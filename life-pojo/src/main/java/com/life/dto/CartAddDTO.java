package com.life.dto;

import lombok.Data;

/**
 * 加购DTO
 */
@Data
public class CartAddDTO {

    /**
     * 商品类型: 1-实物商品, 2-虚拟权益, 必填
     */
    private Integer itemType;

    /**
     * 商品ID/权益ID, 必填
     */
    private Long itemId;

    /**
     * 数量, 默认1
     */
    private Integer quantity;
}

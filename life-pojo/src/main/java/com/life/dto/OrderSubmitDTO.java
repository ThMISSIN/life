package com.life.dto;

import lombok.Data;

import java.util.List;

/**
 * 提交订单DTO
 */
@Data
public class OrderSubmitDTO {

    /**
     * 订单商品明细, 必填
     */
    private List<OrderItemDTO> items;

    /**
     * 收货地址快照(下单时选中的地址), 实物商品必填
     */
    private Long addressId;

    /**
     * 订单备注
     */
    private String remark;
}

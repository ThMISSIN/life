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

    /**
     * 来源购物车行ID(可选): 从购物车结算时传, 下单成功后同事务清理这些行;
     * 直购路径(商品详情页立即购买)不传
     */
    private List<Long> cartIds;
}

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

    /**
     * 积分抵扣(可选, 仅商品订单): 抵扣积分数, 需为配置 rate 的整数倍,
     * 抵扣金额(分)=points/rate*100, 上限为订单现金金额的 maxRatio%(2.5)
     */
    private Integer pointsDeduct;

    /**
     * 下单幂等令牌(必填): 进入结算/兑换时经 GET /order/token 签发(状态 INIT),
     * 提交时原子流转 INIT->USED 获得唯一处理权; 下单成功后令牌永久失效, 失败还原 INIT 允许同令牌重试
     */
    private String orderToken;
}

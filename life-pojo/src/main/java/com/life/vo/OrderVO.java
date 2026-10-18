package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 订单类型: 1-商品订单, 2-权益订单
     */
    private Integer orderType;

    /**
     * 订单状态: 0-待支付, 1-已支付, 2-已发货, 3-已完成, 4-已取消, 5-已退款
     */
    private Integer status;

    /**
     * 下单用户ID
     */
    private Long userId;

    /**
     * 下单用户昵称(仅管理端列表填充)
     */
    private String nickname;

    /**
     * 总金额(分)
     */
    private Integer totalAmount;

    /**
     * 实付金额(分)
     */
    private Integer payAmount;

    /**
     * 使用积分
     */
    private Integer pointsUsed;

    /**
     * 支付渠道
     */
    private String payChannel;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 收货地址(JSON)
     */
    private String deliveryAddress;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 下单时间
     */
    private LocalDateTime createTime;

    /**
     * 订单明细
     */
    private List<OrderDetailVO> details;
}

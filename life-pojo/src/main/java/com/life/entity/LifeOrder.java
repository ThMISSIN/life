package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_order")
public class LifeOrder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单类型: 1-商品订单(现金/积分/混合支付), 2-权益订单(积分兑换)
     */
    private Integer orderType;

    /**
     * 订单状态: 0-待支付, 1-已支付, 2-已发货, 3-已完成, 4-已取消, 5-已退款
     */
    private Integer status;

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
}

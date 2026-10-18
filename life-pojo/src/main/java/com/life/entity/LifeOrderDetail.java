package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单详情表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_order_detail")
public class LifeOrderDetail extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

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

package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 购物车表(快照式: 名称/图片/单价加购时冗余, 列表查询时再关联实时状态)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_cart")
public class LifeCart extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品类型: 1-实物商品, 2-虚拟权益
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
}

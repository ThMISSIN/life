package com.ruoyi.life.domain;

/**
 * 订单明细对象 life_order_detail
 *
 * @author life
 */
public class LifeOrderDetail extends LifeBaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 商品类型: 1-实物商品, 2-虚拟权益 */
    private Integer itemType;

    /** 商品ID/权益ID */
    private Long itemId;

    /** 商品名称 */
    private String itemName;

    /** 商品图片 */
    private String itemImage;

    /** 单价(分) */
    private Integer price;

    /** 数量 */
    private Integer quantity;

    /** 总金额(分) */
    private Integer totalAmount;

    /** 积分价格 */
    private Integer pointsPrice;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public Integer getItemType()
    {
        return itemType;
    }

    public void setItemType(Integer itemType)
    {
        this.itemType = itemType;
    }

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public String getItemName()
    {
        return itemName;
    }

    public void setItemName(String itemName)
    {
        this.itemName = itemName;
    }

    public String getItemImage()
    {
        return itemImage;
    }

    public void setItemImage(String itemImage)
    {
        this.itemImage = itemImage;
    }

    public Integer getPrice()
    {
        return price;
    }

    public void setPrice(Integer price)
    {
        this.price = price;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public Integer getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public Integer getPointsPrice()
    {
        return pointsPrice;
    }

    public void setPointsPrice(Integer pointsPrice)
    {
        this.pointsPrice = pointsPrice;
    }
}

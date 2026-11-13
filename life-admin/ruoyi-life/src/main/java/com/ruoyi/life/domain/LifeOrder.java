package com.ruoyi.life.domain;

import com.ruoyi.common.annotation.Excel;
import java.util.Date;
import java.util.List;

/**
 * 订单对象 life_order（管理端列表含下单人昵称与订单明细, 非表字段标注）
 *
 * @author life
 */
public class LifeOrder extends LifeBaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 下单人昵称(查询展示字段, 非表字段) */
    @Excel(name = "下单人")
    private String nickname;

    /** 订单类型: 1-商品订单(现金/积分/混合支付), 2-权益订单(积分兑换) */
    @Excel(name = "订单类型", readConverterExp = "1=商品订单,2=权益订单")
    private Integer orderType;

    /** 订单状态: 0-待支付, 1-已支付, 2-已发货, 3-已完成, 4-已取消, 5-已退款 */
    @Excel(name = "订单状态", readConverterExp = "0=待支付,1=已支付,2=已发货,3=已完成,4=已取消,5=已退款")
    private Integer status;

    /** 总金额(分) */
    @Excel(name = "总金额(分)")
    private Integer totalAmount;

    /** 实付金额(分) */
    @Excel(name = "实付金额(分)")
    private Integer payAmount;

    /** 使用积分 */
    @Excel(name = "使用积分")
    private Integer pointsUsed;

    /** 支付渠道 */
    private String payChannel;

    /** 支付时间 */
    private Date payTime;

    /** 收货地址(JSON) */
    private String deliveryAddress;

    /** 订单备注 */
    private String remark;

    /** 订单明细(查询展示字段, 非表字段) */
    private List<LifeOrderDetail> details;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public Integer getOrderType()
    {
        return orderType;
    }

    public void setOrderType(Integer orderType)
    {
        this.orderType = orderType;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public Integer getPayAmount()
    {
        return payAmount;
    }

    public void setPayAmount(Integer payAmount)
    {
        this.payAmount = payAmount;
    }

    public Integer getPointsUsed()
    {
        return pointsUsed;
    }

    public void setPointsUsed(Integer pointsUsed)
    {
        this.pointsUsed = pointsUsed;
    }

    public String getPayChannel()
    {
        return payChannel;
    }

    public void setPayChannel(String payChannel)
    {
        this.payChannel = payChannel;
    }

    public Date getPayTime()
    {
        return payTime;
    }

    public void setPayTime(Date payTime)
    {
        this.payTime = payTime;
    }

    public String getDeliveryAddress()
    {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress)
    {
        this.deliveryAddress = deliveryAddress;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public List<LifeOrderDetail> getDetails()
    {
        return details;
    }

    public void setDetails(List<LifeOrderDetail> details)
    {
        this.details = details;
    }
}

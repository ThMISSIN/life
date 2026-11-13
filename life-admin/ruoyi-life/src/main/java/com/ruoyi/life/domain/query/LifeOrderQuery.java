package com.ruoyi.life.domain.query;

/**
 * 订单分页查询（迁移自C端 OrderQueryDTO）
 */
public class LifeOrderQuery
{
    /** 订单状态: 0-待支付, 1-已支付, 2-已发货, 3-已完成, 4-已取消, 5-已退款; 不传查全部 */
    private Integer status;

    /** 订单类型: 1-商品订单, 2-权益订单; 不传查全部 */
    private Integer orderType;

    /** 用户ID */
    private Long userId;

    /** 下单时间起(yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss) */
    private String startTime;

    /** 下单时间止 */
    private String endTime;

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getOrderType()
    {
        return orderType;
    }

    public void setOrderType(Integer orderType)
    {
        this.orderType = orderType;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getStartTime()
    {
        return startTime;
    }

    public void setStartTime(String startTime)
    {
        this.startTime = startTime;
    }

    public String getEndTime()
    {
        return endTime;
    }

    public void setEndTime(String endTime)
    {
        this.endTime = endTime;
    }
}

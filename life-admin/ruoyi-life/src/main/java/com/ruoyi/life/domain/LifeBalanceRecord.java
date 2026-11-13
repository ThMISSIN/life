package com.ruoyi.life.domain;

import com.ruoyi.common.annotation.Excel;
import java.util.Date;

/**
 * 余额流水对象 life_balance_record
 *
 * @author life
 */
public class LifeBalanceRecord extends LifeBaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户昵称(查询展示字段, 非表字段) */
    @Excel(name = "用户昵称")
    private String nickname;

    /** 变动金额(分, 正数为入账, 负数为出账) */
    @Excel(name = "变动金额(分)")
    private Integer amount;

    /** 类型: 1-充值, 2-消费, 3-退款, 4-管理调整 */
    @Excel(name = "类型", readConverterExp = "1=充值,2=消费,3=退款,4=管理调整")
    private Integer type;

    /** 关联订单号 */
    @Excel(name = "关联订单号")
    private String orderNo;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
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

    public Integer getAmount()
    {
        return amount;
    }

    public void setAmount(Integer amount)
    {
        this.amount = amount;
    }

    public Integer getType()
    {
        return type;
    }

    public void setType(Integer type)
    {
        this.type = type;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}

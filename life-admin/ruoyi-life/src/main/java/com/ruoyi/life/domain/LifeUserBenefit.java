package com.ruoyi.life.domain;

import com.ruoyi.common.annotation.Excel;
import java.util.Date;

/**
 * 用户权益(发放记录)对象 life_user_benefit
 *
 * @author life
 */
public class LifeUserBenefit extends LifeBaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Excel(name = "记录ID", sort = 1)
    private Long id;

    /** 用户ID */
    @Excel(name = "用户ID", sort = 2)
    private Long userId;

    /** 用户昵称(查询展示字段, 非表字段) */
    @Excel(name = "用户昵称", sort = 3)
    private String nickname;

    /** 权益ID */
    private Long benefitId;

    /** 权益名称快照 */
    @Excel(name = "权益名称", sort = 4)
    private String benefitName;

    /** 封面图快照 */
    private String coverImage;

    /** 关联订单号 */
    @Excel(name = "关联订单号", sort = 7)
    private String orderNo;

    /** 券码 */
    @Excel(name = "券码", sort = 5)
    private String code;

    /** 生效时间 */
    @Excel(name = "生效时间", dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 8)
    private Date effectiveTime;

    /** 过期时间 */
    @Excel(name = "过期时间", dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 9)
    private Date expireTime;

    /** 核销(使用)时间 */
    @Excel(name = "核销时间", dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 10)
    private Date verifyTime;

    /** 核销操作人ID */
    private Long verifyOperator;

    /** 状态: 0-待生效, 1-未使用, 2-已使用, 3-已过期, 4-已作废 */
    @Excel(name = "状态", readConverterExp = "0=待生效,1=未使用,2=已使用,3=已过期,4=已作废", sort = 6)
    private Integer status;

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

    public Long getBenefitId()
    {
        return benefitId;
    }

    public void setBenefitId(Long benefitId)
    {
        this.benefitId = benefitId;
    }

    public String getBenefitName()
    {
        return benefitName;
    }

    public void setBenefitName(String benefitName)
    {
        this.benefitName = benefitName;
    }

    public String getCoverImage()
    {
        return coverImage;
    }

    public void setCoverImage(String coverImage)
    {
        this.coverImage = coverImage;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public Date getEffectiveTime()
    {
        return effectiveTime;
    }

    public void setEffectiveTime(Date effectiveTime)
    {
        this.effectiveTime = effectiveTime;
    }

    public Date getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(Date expireTime)
    {
        this.expireTime = expireTime;
    }

    public Date getVerifyTime()
    {
        return verifyTime;
    }

    public void setVerifyTime(Date verifyTime)
    {
        this.verifyTime = verifyTime;
    }

    public Long getVerifyOperator()
    {
        return verifyOperator;
    }

    public void setVerifyOperator(Long verifyOperator)
    {
        this.verifyOperator = verifyOperator;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }
}

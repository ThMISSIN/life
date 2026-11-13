package com.ruoyi.life.domain.query;

/**
 * 用户权益(发放记录)分页查询（迁移自C端 UserBenefitQueryDTO, 管理端口径;
 * 1.7 筛选增强: 权益名称/用户关键词/获得时间区间）
 */
public class LifeUserBenefitQuery
{
    /** 权益状态: 0-待生效, 1-未使用, 2-已使用, 3-已过期, 4-已作废; 不传查全部
     *  说明: 过期无落库状态, 按 未使用+expire_time<now 实时推导 */
    private Integer status;

    /** 用户ID */
    private Long userId;

    /** 权益ID */
    private Long benefitId;

    /** 订单号精确查 */
    private String orderNo;

    /** 券码精确查 */
    private String code;

    /** 权益名称关键词(发放时快照列模糊查) */
    private String benefitName;

    /** 用户关键词(昵称/用户名模糊查) */
    private String userKeyword;

    /** 获得时间-起(yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss, 服务层归一化) */
    private String beginTime;

    /** 获得时间-止 */
    private String endTime;

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getBenefitId()
    {
        return benefitId;
    }

    public void setBenefitId(Long benefitId)
    {
        this.benefitId = benefitId;
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

    public String getBenefitName()
    {
        return benefitName;
    }

    public void setBenefitName(String benefitName)
    {
        this.benefitName = benefitName;
    }

    public String getUserKeyword()
    {
        return userKeyword;
    }

    public void setUserKeyword(String userKeyword)
    {
        this.userKeyword = userKeyword;
    }

    public String getBeginTime()
    {
        return beginTime;
    }

    public void setBeginTime(String beginTime)
    {
        this.beginTime = beginTime;
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

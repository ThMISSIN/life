package com.ruoyi.life.domain.query;

/**
 * 通知模板分页查询
 */
public class LifeNoticeTemplateQuery
{
    /** 模板名称(模糊) */
    private String name;

    /** 模板类型: 1-系统, 2-订单, 3-活动, 4-权益, 5-积分; 不传查全部 */
    private Integer type;

    /** 状态: 0-禁用, 1-启用; 不传查全部 */
    private Integer status;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Integer getType()
    {
        return type;
    }

    public void setType(Integer type)
    {
        this.type = type;
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

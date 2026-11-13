package com.ruoyi.life.domain;

/**
 * 通知模板对象 life_notice_template
 *
 * @author life
 */
public class LifeNoticeTemplate extends LifeBaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模板ID */
    private Long id;

    /** 模板名称 */
    private String name;

    /** 模板编码(唯一, 业务方按code取模板渲染) */
    private String code;

    /** 模板类型: 1-系统通知, 2-订单通知, 3-活动通知, 4-权益通知, 5-积分通知 */
    private Integer type;

    /** 标题模板(支持${变量}占位) */
    private String titleTemplate;

    /** 内容模板(支持${变量}占位) */
    private String contentTemplate;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public Integer getType()
    {
        return type;
    }

    public void setType(Integer type)
    {
        this.type = type;
    }

    public String getTitleTemplate()
    {
        return titleTemplate;
    }

    public void setTitleTemplate(String titleTemplate)
    {
        this.titleTemplate = titleTemplate;
    }

    public String getContentTemplate()
    {
        return contentTemplate;
    }

    public void setContentTemplate(String contentTemplate)
    {
        this.contentTemplate = contentTemplate;
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

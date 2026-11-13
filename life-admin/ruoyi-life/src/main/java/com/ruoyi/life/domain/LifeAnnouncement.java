package com.ruoyi.life.domain;

import java.util.Date;

/**
 * 系统公告对象 life_announcement
 *
 * @author life
 */
public class LifeAnnouncement extends LifeBaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 公告ID */
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 公告类型: 1-系统公告, 2-活动公告, 3-权益公告 */
    private Integer type;

    /** 是否置顶: 0-否, 1-是 */
    private Integer top;

    /** 状态: 0-草稿, 1-已发布, 2-已撤回 */
    private Integer status;

    /** 发布时间 */
    private Date publishTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Integer getType()
    {
        return type;
    }

    public void setType(Integer type)
    {
        this.type = type;
    }

    public Integer getTop()
    {
        return top;
    }

    public void setTop(Integer top)
    {
        this.top = top;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Date getPublishTime()
    {
        return publishTime;
    }

    public void setPublishTime(Date publishTime)
    {
        this.publishTime = publishTime;
    }
}

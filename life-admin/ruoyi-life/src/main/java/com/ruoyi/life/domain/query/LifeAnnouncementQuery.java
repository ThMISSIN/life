package com.ruoyi.life.domain.query;

/**
 * 系统公告分页查询
 */
public class LifeAnnouncementQuery
{
    /** 标题关键词(模糊) */
    private String keyword;

    /** 公告类型: 1-系统公告, 2-活动公告, 3-权益公告; 不传查全部 */
    private Integer type;

    /** 状态: 0-草稿, 1-已发布, 2-已撤回; 不传查全部 */
    private Integer status;

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
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

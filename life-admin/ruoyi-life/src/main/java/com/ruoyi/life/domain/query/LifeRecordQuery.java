package com.ruoyi.life.domain.query;

/**
 * 流水分页查询（迁移自C端 RecordQueryDTO, 积分流水/余额流水共用）
 */
public class LifeRecordQuery
{
    /** 用户ID, 不传查全部 */
    private Long userId;

    /** 流水类型, 不传查全部 */
    private Integer type;

    /** 时间起(yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss) */
    private String startTime;

    /** 时间止 */
    private String endTime;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Integer getType()
    {
        return type;
    }

    public void setType(Integer type)
    {
        this.type = type;
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

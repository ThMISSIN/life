package com.ruoyi.life.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * life业务表基类（与C端 com.life.entity.BaseEntity 字段对齐）
 * life_social 库业务表公共列: create_time / update_time / creater / updater / deleted
 */
public class LifeBaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 创建者ID */
    private Long creater;

    /** 更新者ID */
    private Long updater;

    /** 删除标志: 0-未删除, 1-已删除(逻辑删除) */
    private Integer deleted;

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public Long getCreater()
    {
        return creater;
    }

    public void setCreater(Long creater)
    {
        this.creater = creater;
    }

    public Long getUpdater()
    {
        return updater;
    }

    public void setUpdater(Long updater)
    {
        this.updater = updater;
    }

    public Integer getDeleted()
    {
        return deleted;
    }

    public void setDeleted(Integer deleted)
    {
        this.deleted = deleted;
    }
}

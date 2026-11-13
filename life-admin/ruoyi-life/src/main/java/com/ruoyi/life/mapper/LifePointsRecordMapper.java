package com.ruoyi.life.mapper;

import com.ruoyi.life.domain.LifePointsRecord;
import com.ruoyi.life.domain.query.LifeRecordQuery;

import java.util.List;

/**
 * 积分流水 数据层
 */
public interface LifePointsRecordMapper
{
    /** 管理端分页查询(关联用户昵称, 时间降序) */
    public List<LifePointsRecord> selectLifePointsRecordList(LifeRecordQuery query);

    /** 新增流水(作废退分写入) */
    public int insertLifePointsRecord(LifePointsRecord lifePointsRecord);
}

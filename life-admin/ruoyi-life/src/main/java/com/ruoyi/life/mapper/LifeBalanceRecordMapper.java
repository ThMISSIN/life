package com.ruoyi.life.mapper;

import com.ruoyi.life.domain.LifeBalanceRecord;
import com.ruoyi.life.domain.query.LifeRecordQuery;

import java.util.List;

/**
 * 余额流水 数据层
 */
public interface LifeBalanceRecordMapper
{
    /** 管理端分页查询(关联用户昵称, 时间降序) */
    public List<LifeBalanceRecord> selectLifeBalanceRecordList(LifeRecordQuery query);
}

package com.ruoyi.life.service;

import com.ruoyi.life.domain.LifeBalanceRecord;
import com.ruoyi.life.domain.query.LifeRecordQuery;

import java.util.List;

/**
 * 余额流水管理 服务接口（迁移自C端 LifeBalanceRecordService 管理端方法）
 */
public interface ILifeBalanceRecordService
{
    /** 分页查询: 按用户/类型/时间区间筛选, 时间降序 */
    public List<LifeBalanceRecord> selectLifeBalanceRecordList(LifeRecordQuery query);
}

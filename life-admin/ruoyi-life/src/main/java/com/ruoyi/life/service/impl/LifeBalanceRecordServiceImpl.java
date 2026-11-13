package com.ruoyi.life.service.impl;

import com.ruoyi.life.domain.LifeBalanceRecord;
import com.ruoyi.life.domain.query.LifeRecordQuery;
import com.ruoyi.life.mapper.LifeBalanceRecordMapper;
import com.ruoyi.life.service.ILifeBalanceRecordService;
import com.ruoyi.life.util.LifeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 余额流水管理 服务实现（业务口径与C端 LifeBalanceRecordServiceImpl 管理端方法一致）
 *
 * @author life
 */
@Service
public class LifeBalanceRecordServiceImpl implements ILifeBalanceRecordService
{
    @Autowired
    private LifeBalanceRecordMapper lifeBalanceRecordMapper;

    @Override
    public List<LifeBalanceRecord> selectLifeBalanceRecordList(LifeRecordQuery query)
    {
        query.setStartTime(LifeUtils.startAt(query.getStartTime()));
        query.setEndTime(LifeUtils.endAt(query.getEndTime()));
        return lifeBalanceRecordMapper.selectLifeBalanceRecordList(query);
    }
}

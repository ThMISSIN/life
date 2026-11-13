package com.ruoyi.life.service.impl;

import com.ruoyi.life.domain.LifePointsRecord;
import com.ruoyi.life.domain.query.LifeRecordQuery;
import com.ruoyi.life.mapper.LifePointsRecordMapper;
import com.ruoyi.life.service.ILifePointsRecordService;
import com.ruoyi.life.util.LifeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 积分流水管理 服务实现（业务口径与C端 LifePointsRecordServiceImpl 管理端方法一致）
 *
 * @author life
 */
@Service
public class LifePointsRecordServiceImpl implements ILifePointsRecordService
{
    @Autowired
    private LifePointsRecordMapper lifePointsRecordMapper;

    @Override
    public List<LifePointsRecord> selectLifePointsRecordList(LifeRecordQuery query)
    {
        query.setStartTime(LifeUtils.startAt(query.getStartTime()));
        query.setEndTime(LifeUtils.endAt(query.getEndTime()));
        return lifePointsRecordMapper.selectLifePointsRecordList(query);
    }
}

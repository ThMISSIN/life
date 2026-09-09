package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.context.BaseContext;
import com.life.entity.LifePointsRecord;
import com.life.mapper.LifePointsRecordMapper;
import com.life.query.QueryDTO;
import com.life.result.PageResult;
import com.life.service.LifePointsRecordService;
import org.springframework.stereotype.Service;

@Service
public class LifePointsRecordServiceImpl extends ServiceImpl<LifePointsRecordMapper, LifePointsRecord> implements LifePointsRecordService {

    @Override
    public PageResult pageList(QueryDTO queryDTO) {
        Long userId = BaseContext.getCurrentId();

        Page<LifePointsRecord> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<LifePointsRecord> resultPage = baseMapper.queryList(page, userId, queryDTO);

        return new PageResult(resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    public void addSignRecord(Long userId, int points) {
        LifePointsRecord record = LifePointsRecord.builder()
                .userId(userId)
                .points(points)
                .type(1)
                .build();
        save(record);
    }
}

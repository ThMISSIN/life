package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.entity.LifePointsRecord;
import com.life.query.QueryDTO;
import com.life.result.PageResult;

public interface LifePointsRecordService extends IService<LifePointsRecord> {
    PageResult pageList(QueryDTO queryDTO);

    void addSignRecord(Long userId, int points);

}

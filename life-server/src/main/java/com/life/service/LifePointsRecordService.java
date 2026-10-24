package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.entity.LifePointsRecord;
import com.life.query.QueryDTO;
import com.life.query.RecordQueryDTO;
import com.life.result.PageResult;

public interface LifePointsRecordService extends IService<LifePointsRecord> {
    PageResult pageList(QueryDTO queryDTO);

    void addSignRecord(Long userId, int points);

    /**
     * 管理端: 积分流水分页(按用户/类型/时间区间筛选, 关联昵称, 对账依赖)
     */
    PageResult queryAdminPage(RecordQueryDTO query);

}

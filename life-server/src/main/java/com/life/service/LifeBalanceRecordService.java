package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.entity.LifeBalanceRecord;
import com.life.query.RecordQueryDTO;
import com.life.result.PageResult;

public interface LifeBalanceRecordService extends IService<LifeBalanceRecord> {

    /**
     * 管理端: 余额流水分页(按用户/类型/时间区间筛选, 关联昵称, 对账依赖)
     */
    PageResult queryAdminPage(RecordQueryDTO query);
}

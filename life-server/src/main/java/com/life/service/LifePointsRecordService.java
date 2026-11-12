package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.entity.LifePointsRecord;
import com.life.query.QueryDTO;
import com.life.query.RecordQueryDTO;
import com.life.result.PageResult;

public interface LifePointsRecordService extends IService<LifePointsRecord> {
    PageResult pageList(QueryDTO queryDTO);

    /**
     * 签到奖励入账(2.2 收编): 走 PointsService.changePoints 统一入口,
     * bizNo=SIGN:{userId}:{yyyyMMdd} 幂等 —— MQ at-least-once 重投不会重复到账
     */
    void addSignRecord(Long userId, int points, String bizNo);

    /**
     * 管理端: 积分流水分页(按用户/类型/时间区间筛选, 关联昵称, 对账依赖)
     */
    PageResult queryAdminPage(RecordQueryDTO query);

}

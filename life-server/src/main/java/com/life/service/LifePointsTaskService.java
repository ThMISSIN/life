package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.entity.LifePointsTask;
import com.life.query.QueryDTO;
import com.life.result.PageResult;

public interface LifePointsTaskService extends IService<LifePointsTask> {

    /**
     * C端只读任务列表(1.6): 上架(status=1)且处于生效时间窗内的任务分页;
     * 领取奖励属二期(路线图 2.4, 依赖 life_points_task_record 防刷表)
     */
    PageResult queryCEndPage(QueryDTO query);
}

package com.life.service;

import com.life.vo.PointsTaskVO;

import java.util.List;

/**
 * 2.4 任务中心: 事件触发完成记录 + 领取奖励(幂等) + 列表进度回填
 */
public interface TaskCenterService {

    /**
     * 业务事件触发: 匹配上架且时间窗内的同类型任务, 未达每日限次则落完成记录(status=0 待领取);
     * uk_user_task_date_seq 撞键=并发重复触发, 静默跳过。业务方同步调用, 内部吞异常不阻断主流程
     *
     * @param eventType TaskEventTypeConstant
     */
    void triggerEvent(Long userId, int eventType);

    /**
     * 领取奖励: 取该任务最早一条待领取记录 → CAS 置已领取(并发双击只成功一次)
     * → MQ 异步发放积分(bizNo=TASKCLAIM:{recordId} 幂等)
     *
     * @return 本次领取的积分数
     */
    int claim(Long userId, Long taskId);

    /**
     * 任务列表进度回填: 今日完成次数 / 待领取次数(批量两条聚合SQL, 防N+1)
     */
    void fillProgress(List<PointsTaskVO> vos, Long userId);
}

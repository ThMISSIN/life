package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.life.constant.MqConstant;
import com.life.constant.TaskEventTypeConstant;
import com.life.dto.MqPointsMessage;
import com.life.entity.LifePointsTask;
import com.life.entity.LifePointsTaskRecord;
import com.life.exception.ValidationException;
import com.life.mapper.LifePointsTaskMapper;
import com.life.mapper.LifePointsTaskRecordMapper;
import com.life.mq.MqProducer;
import com.life.service.TaskCenterService;
import com.life.vo.PointsTaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2.4 任务中心实现
 * <p>完成与发放分离: 业务事件同步落"完成记录"(uk 幂等), 用户主动领取时 CAS 流转 +
 * MQ 异步发放(2.12 口径); 奖励额取完成时的任务快照(reward_points), 运营改分不影响已完成记录
 */
@Service
@Slf4j
public class TaskCenterServiceImpl implements TaskCenterService {

    @Autowired
    private LifePointsTaskMapper taskMapper;
    @Autowired
    private LifePointsTaskRecordMapper recordMapper;
    @Autowired
    private MqProducer mqProducer;
    /** MQ 不可用时的同步发放降级通道 */
    @Autowired
    private com.life.service.PointsService pointsService;

    @Override
    public void triggerEvent(Long userId, int eventType) {
        if (userId == null) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            List<LifePointsTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<LifePointsTask>()
                    .eq(LifePointsTask::getStatus, 1)
                    .eq(LifePointsTask::getType, eventType)
                    .and(w -> w.isNull(LifePointsTask::getStartTime).or().le(LifePointsTask::getStartTime, now))
                    .and(w -> w.isNull(LifePointsTask::getEndTime).or().ge(LifePointsTask::getEndTime, now)));
            if (tasks.isEmpty()) {
                return;
            }
            LocalDate today = LocalDate.now();
            for (LifePointsTask task : tasks) {
                int limit = task.getDailyLimit() == null || task.getDailyLimit() < 1 ? 1 : task.getDailyLimit();
                if (eventType == TaskEventTypeConstant.PROFILE) {
                    // 一次性任务(首次完善资料): 终身一次, 不限当日
                    Long ever = recordMapper.selectCount(new LambdaQueryWrapper<LifePointsTaskRecord>()
                            .eq(LifePointsTaskRecord::getUserId, userId)
                            .eq(LifePointsTaskRecord::getTaskId, task.getId()));
                    if (ever != null && ever > 0) {
                        continue;
                    }
                } else if (recordMapper.countToday(userId, task.getId(), today) >= limit) {
                    continue;
                }
                try {
                    recordMapper.insert(LifePointsTaskRecord.builder()
                            .userId(userId)
                            .taskId(task.getId())
                            .taskDate(today)
                            .seq(recordMapper.countToday(userId, task.getId(), today) + 1)
                            .status(0)
                            .rewardPoints(task.getPoints())
                            .build());
                } catch (DuplicateKeyException dup) {
                    // 并发/重复事件撞 uk_user_task_date_seq: 幂等跳过
                    log.debug("任务完成记录重复, 跳过 userId={} taskId={}", userId, task.getId());
                }
            }
        } catch (Exception e) {
            // 任务完成属附加激励, 失败不阻断签到/评论/转发主流程
            log.warn("任务事件触发失败 userId={} eventType={}", userId, eventType, e);
        }
    }

    @Override
    public int claim(Long userId, Long taskId) {
        LifePointsTaskRecord record = recordMapper.selectFirstClaimable(userId, taskId);
        if (record == null) {
            throw new ValidationException("暂无可领取的任务奖励");
        }
        // CAS 0→1: 并发双击只有一个请求能流转成功
        if (recordMapper.markClaimed(record.getId(), userId) == 0) {
            throw new ValidationException("奖励已被领取, 请勿重复操作");
        }
        LifePointsTask task = taskMapper.selectById(taskId);
        // MQ 异步发放(2.12): bizNo=TASKCLAIM:{recordId}, 重投由 uk_biz_no 幂等拦截;
        // broker 不可用时降级同步发放, 保证用户点了就有分
        MqPointsMessage msg = MqPointsMessage.builder()
                .userId(userId)
                .points(record.getRewardPoints())
                .bizNo("TASKCLAIM:" + record.getId())
                .remark("任务奖励: " + (task == null ? taskId : task.getName()))
                .build();
        if (!mqProducer.sendQuiet(MqConstant.POINTS_EXCHANGE, MqConstant.POINTS_TASK_ROUTING_KEY, msg)) {
            log.warn("任务奖励MQ不可用, 降级同步发放 bizNo={}", msg.getBizNo());
            pointsService.changePoints(userId, record.getRewardPoints(),
                    com.life.constant.PointsTypeConstant.ACTIVITY, msg.getBizNo(), msg.getRemark());
        }
        return record.getRewardPoints();
    }

    @Override
    public void fillProgress(List<PointsTaskVO> vos, Long userId) {
        if (vos == null || vos.isEmpty() || userId == null) {
            return;
        }
        List<Long> taskIds = vos.stream().map(PointsTaskVO::getId).toList();
        Map<Long, Integer> todayMap = toCountMap(recordMapper.countTodayByTasks(userId, LocalDate.now(), taskIds));
        Map<Long, Integer> claimMap = toCountMap(recordMapper.countClaimableByTasks(userId, taskIds));
        for (PointsTaskVO vo : vos) {
            vo.setTodayCompleted(todayMap.getOrDefault(vo.getId(), 0));
            vo.setClaimable(claimMap.getOrDefault(vo.getId(), 0));
        }
    }

    private Map<Long, Integer> toCountMap(List<Map<String, Object>> rows) {
        Map<Long, Integer> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            Object taskId = row.get("taskId");
            Object cnt = row.get("cnt");
            if (taskId != null && cnt != null) {
                result.put(((Number) taskId).longValue(), ((Number) cnt).intValue());
            }
        }
        return result;
    }
}

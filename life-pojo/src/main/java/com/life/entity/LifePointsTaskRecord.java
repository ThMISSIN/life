package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 积分任务完成/领取记录(2.4/3.2)
 * <p>幂等: uk_user_task_date_seq(user_id, task_id, task_date, seq) —— 每日限 daily_limit 次,
 * 事件重复触发撞唯一键静默跳过
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_points_task_record")
public class LifePointsTaskRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long taskId;

    /** 完成日期(每日任务=当天, 一次性任务=完成日) */
    private LocalDate taskDate;

    /** 当日第几次完成(1..daily_limit) */
    private Integer seq;

    /** 状态: 0-已完成待领取, 1-已领取 */
    private Integer status;

    /** 完成时奖励积分快照(防任务改分后领取口径漂移) */
    private Integer rewardPoints;
}

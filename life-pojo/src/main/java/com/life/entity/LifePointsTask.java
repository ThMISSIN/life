package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分任务表
 */
@Data
@TableName("life_points_task")
public class LifePointsTask extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务类型: 1-每日签到, 2-消费奖励, 3-邀请好友, 4-评价奖励, 5-活动任务
     */
    private Integer type;

    /**
     * 奖励积分
     */
    private Integer points;

    /**
     * 每日次数限制
     */
    private Integer dailyLimit;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态: 0-禁用, 1-正常
     */
    private Integer status;
}

package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 积分任务 C端展示VO(上架任务的只读视图, 领取奖励属二期)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsTaskVO implements Serializable {

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
}

package com.life.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * MQ积分发放报文(2.12): 签到/任务奖励统一载体;
 * bizNo 为幂等键(消费端 changePoints 判重), at-least-once 重投安全
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqPointsMessage implements Serializable {

    private Long userId;

    /** 发放积分(正数) */
    private Integer points;

    /** 幂等业务号: SIGN:{userId}:{yyyyMMdd} / TASKCLAIM:{recordId} */
    private String bizNo;

    private String remark;
}

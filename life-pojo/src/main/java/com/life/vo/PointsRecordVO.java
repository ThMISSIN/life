package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 积分流水VO(管理端列表, 带用户昵称)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsRecordVO {

    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称(管理端列表批量填充)
     */
    private String nickname;

    /**
     * 积分值(正数为获取, 负数为消耗)
     */
    private Integer points;

    /**
     * 积分类型: 1-签到, 2-消费, 3-活动奖励, 4-邀请好友, 5-评价奖励, 6-积分兑换, 7-兑换作废退回
     */
    private Integer type;

    /**
     * 关联订单号
     */
    private String orderNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 发生时间
     */
    private LocalDateTime createTime;
}

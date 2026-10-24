package com.life.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流水分页查询DTO(管理端积分流水/余额流水共用)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RecordQueryDTO extends QueryDTO {

    /**
     * 用户ID, 不传查全部
     */
    private Long userId;

    /**
     * 流水类型, 不传查全部
     * 积分流水: 1-签到, 2-消费, 3-活动奖励, 4-邀请好友, 5-评价奖励, 6-积分兑换, 7-兑换作废退回
     * 余额流水: 1-充值, 2-消费, 3-退款, 4-管理调整
     */
    private Integer type;
}

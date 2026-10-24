package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分总览VO — 字段名即前端契约: api/user.js getPointsSummary() 返回 {total, monthEarned, monthSpent}, 勿改名
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsSummaryVO {

    /**
     * 当前积分余额(life_user.points 账户字段为准, 不是流水求和)
     */
    private Integer total;

    /**
     * 本月获得(本月正数流水之和)
     */
    private Integer monthEarned;

    /**
     * 本月消耗(本月负数流水之和的绝对值, 返回正数)
     */
    private Integer monthSpent;
}

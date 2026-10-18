package com.life.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户权益分页查询DTO(C端我的权益/管理端发放记录共用)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserBenefitQueryDTO extends QueryDTO {

    /**
     * 权益状态: 0-待生效, 1-未使用, 2-已使用, 3-已过期, 4-已作废; 不传查全部
     * 说明: 过期无落库状态, 由 未使用+expire_time<now 实时推导
     */
    private Integer status;

    /**
     * 用户ID, 仅管理端筛选生效, C端强制当前登录用户
     */
    private Long userId;

    /**
     * 权益ID, 仅管理端筛选生效
     */
    private Long benefitId;

    /**
     * 订单号精确查, 仅管理端生效
     */
    private String orderNo;

    /**
     * 券码精确查(核销前定位), 仅管理端生效
     */
    private String code;
}

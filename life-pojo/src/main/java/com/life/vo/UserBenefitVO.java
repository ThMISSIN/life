package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户权益VO: C端列表不下发code, 详情与管理端发放记录含code
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBenefitVO {

    private Long id;

    /**
     * 用户ID(管理端视图字段)
     */
    private Long userId;

    /**
     * 权益ID
     */
    private Long benefitId;

    /**
     * 权益名称快照
     */
    private String benefitName;

    /**
     * 封面图快照
     */
    private String coverImage;

    /**
     * 关联订单号
     */
    private String orderNo;

    /**
     * 券码
     */
    private String code;

    /**
     * 生效时间
     */
    private LocalDateTime effectiveTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 核销时间
     */
    private LocalDateTime verifyTime;

    /**
     * 核销操作人ID
     */
    private Long verifyOperator;

    /**
     * 状态: 0-待生效, 1-未使用, 2-已使用, 3-已过期(实时推导), 4-已作废
     */
    private Integer status;

    /**
     * 领取(发放)时间
     */
    private LocalDateTime createTime;
}

package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户权益表
 */
@Data
@TableName("life_user_benefit")
public class LifeUserBenefit extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
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
     * 核销(使用)时间
     */
    private LocalDateTime verifyTime;

    /**
     * 核销操作人ID
     */
    private Long verifyOperator;

    /**
     * 状态: 0-待生效, 1-未使用, 2-已使用, 3-已过期, 4-已作废
     */
    private Integer status;
}

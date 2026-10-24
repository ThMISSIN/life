package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 余额流水VO(管理端列表, 带下单人昵称)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceRecordVO {

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
     * 变动金额(分), 正数为入账, 负数为出账
     */
    private Integer amount;

    /**
     * 类型: 1-充值, 2-消费, 3-退款, 4-管理调整
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

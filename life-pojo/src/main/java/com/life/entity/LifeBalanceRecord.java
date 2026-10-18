package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 余额流水表(与 life_points_record 同构: 正数为入账, 负数为出账)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_balance_record")
public class LifeBalanceRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 变动金额(分, 正数为入账, 负数为出账)
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
}

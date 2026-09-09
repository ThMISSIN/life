package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分记录表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_points_record")
public class LifePointsRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 积分值(正数为获取, 负数为消耗)
     */
    private Integer points;

    /**
     * 积分类型: 1-签到, 2-消费, 3-活动奖励, 4-邀请好友, 5-评价奖励, 6-积分兑换
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

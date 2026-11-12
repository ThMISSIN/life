package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
     * 积分类型: 1-签到, 2-消费, 3-活动奖励, 4-邀请好友, 5-评价奖励, 6-积分兑换, 7-兑换作废退回, 8-榜单奖励, 9-过期回收
     */
    private Integer type;

    /**
     * 流水状态(2.8 状态机): 1-已结算, 2-冻结中, 3-已取消, 4-已过期
     * @see com.life.constant.PointsRecordStatusConstant
     */
    private Integer status;

    /**
     * 关联订单号
     */
    private String orderNo;

    /**
     * 幂等业务号(2.2/3.1): 全局唯一索引 uk_biz_no, 同 bizNo 重复入账被唯一键拦截
     */
    private String bizNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 过期时间(2.8): 正向发放时写入(now + life.points.expire-days), 过期回收任务依据
     */
    private LocalDateTime expireTime;
}

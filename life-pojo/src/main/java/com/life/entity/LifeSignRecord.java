package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 签到落库记录(3.5): Redis bitmap 仍是连签计算主存, 本表持久化兜底;
 * uk_user_date 唯一键 → INSERT IGNORE 天然幂等, 重复签到/补写不产生双行
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_sign_record")
public class LifeSignRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate signDate;

    /** 当日连签天数 */
    private Integer continueDays;

    /** 本次应发积分(基础+连签奖励, MQ异步入账) */
    private Integer rewardPoints;
}

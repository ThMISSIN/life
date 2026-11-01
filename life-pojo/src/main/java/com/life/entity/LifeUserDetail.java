package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户详情表(1:1 挂 life_user, uk_user_id 唯一; 资料扩展字段+通知偏好设置)
 */
@Data
@TableName("life_user_detail")
public class LifeUserDetail extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * C端用户ID(唯一)
     */
    private Long userId;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 性别: 0-女, 1-男
     */
    private Integer gender;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区
     */
    private String district;

    /**
     * 积分变动提醒: 0-关闭, 1-开启
     */
    private Integer notifyPoints;

    /**
     * 推荐动态: 0-关闭, 1-开启
     */
    private Integer recommendFeed;
}

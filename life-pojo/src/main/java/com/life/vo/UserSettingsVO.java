package com.life.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户设置VO(GET /user/settings) — 无设置记录时返回全默认值(开关均开启)
 */
@Data
public class UserSettingsVO implements Serializable {

    /**
     * 积分变动提醒: 0-关闭, 1-开启
     */
    private Integer notifyPoints = 1;

    /**
     * 推荐动态: 0-关闭, 1-开启
     */
    private Integer recommendFeed = 1;

    /**
     * 性别: 0-女, 1-男
     */
    private Integer gender;

    /**
     * 生日 yyyy-MM-dd
     */
    private String birthday;

    private String address;

    private String province;

    private String city;

    private String district;
}

package com.life.dto;

import lombok.Data;

/**
 * 用户设置保存DTO(PUT /user/settings) — 部分更新: 仅覆盖传值字段
 */
@Data
public class UserSettingsDTO {

    /**
     * 积分变动提醒: 0-关闭, 1-开启
     */
    private Integer notifyPoints;

    /**
     * 推荐动态: 0-关闭, 1-开启
     */
    private Integer recommendFeed;

    /**
     * 性别: 0-女, 1-男
     */
    private Integer gender;

    /**
     * 生日 yyyy-MM-dd
     */
    private String birthday;

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
}

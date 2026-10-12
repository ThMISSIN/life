package com.life.constant;

/**
 * 通用校验正则
 */
public class RegexConstant {

    /** 中国大陆手机号(兼容+86/0086前缀) */
    public static final String PHONE = "^(?:(?:\\+|00)86)?1[3-9]\\d{9}$";

    /** 邮箱 */
    public static final String EMAIL =
            "^(([^<>()\\\\.,;:\\s@\"]+(\\.[^<>()\\\\.,;:\\s@\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}))$";
}

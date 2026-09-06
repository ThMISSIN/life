package com.life.dto;

import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginDTO {

    /**
     * 统一账号：可以是 用户名 / 手机号 / 邮箱
     */
    private String account;

    /**
     * 密码，前端传原始密码，服务端BCrypt校验
     */
    private String password;

    /**
     * 二次确认密码
     */
    private String confirmPassword;


}

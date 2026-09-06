package com.life.dto;

import lombok.Data;

/**
 * 注册请求DTO
 */
@Data
public class RegisterDTO {

    private String username;

    private String phone;

    private String email;

    private String password;
}

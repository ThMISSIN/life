package com.life.dto;

import lombok.Data;

/**
 * 用户信息修改DTO
 */
@Data
public class UserUpdateDTO {

    private String nickname;

    private String avatar;

    private String email;

    private String phone;
}

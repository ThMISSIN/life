package com.life.dto;

import lombok.Data;

/**
 * 修改密码DTO(登录态下凭原密码改新密码, 区别于免登录的忘记密码)
 */
@Data
public class UpdatePasswordDTO {

    /**
     * 原密码(明文)
     */
    private String oldPassword;

    /**
     * 新密码(明文)
     */
    private String newPassword;
}

package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("life_user")
public class LifeUser extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String phone;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private Integer userType;
    private Integer points;
    /** 余额(分) */
    private Integer money;
    private Integer level;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private Integer loginCount;

}

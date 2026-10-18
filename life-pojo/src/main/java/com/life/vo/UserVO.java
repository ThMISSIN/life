package com.life.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户信息VO，登录返回的用户详情
 */
@Data
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户类型: 1-管理员, 2-普通用户, 3‑VIP用户
     */
    private Integer userType;

    /**
     * 积分余额
     */
    private Integer points;

    /**
     * 余额(分)
     */
    private Integer money;

    /**
     * 会员等级:1‑普通会员,2‑银卡,3‑金卡,4‑钻石
     */
    private Integer level;

    /**
     * 账号状态 0‑禁用，1‑正常
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 登录次数
     */
    private Integer loginCount;

    /**
     * 关注数(仅用户主页详情接口返回)
     */
    private Integer followingCount;

    /**
     * 粉丝数(仅用户主页详情接口返回)
     */
    private Integer followerCount;

    /**
     * 当前登录用户是否已关注TA(仅用户主页详情接口返回, 看自己时为null)
     */
    private Boolean followed;
}

package com.life.constant;

/**
 * 用户类型(life_user.user_type): 2.3 管理端鉴权依据
 */
public class UserTypeConstant {

    /** 管理员: 可登录管理端(/admin/**, 独立令牌) */
    public static final int ADMIN = 1;

    /** 普通用户 */
    public static final int NORMAL = 2;

    /** VIP用户 */
    public static final int VIP = 3;
}

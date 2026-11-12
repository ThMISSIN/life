package com.life.service;

import com.life.dto.LoginDTO;
import com.life.result.Result;
import com.life.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 2.3 管理端认证(C端 life-social 内的 /admin/** REST 专用, 与 life-admin RuoYi 的 sys_user 体系无关):
 * 独立密钥/独立请求头签发与校验, 仅 user_type=1 管理员可登录
 */
public interface AdminAuthService {

    /**
     * 管理端登录: 账号密码校验 + userType=ADMIN 校验 → 签发管理端令牌(claims 含 userType)
     */
    LoginVO adminLogin(LoginDTO loginDTO);

    /**
     * 管理端登出: 令牌进黑名单(剩余TTL), 拦截器二次校验拒绝
     */
    Result adminLogout(HttpServletRequest request);
}

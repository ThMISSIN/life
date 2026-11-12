package com.life.controller;

import com.life.dto.LoginDTO;
import com.life.result.Result;
import com.life.service.AdminAuthService;
import com.life.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2.3 管理端认证接口(life-social /admin/** REST 专用登录, 与C端登录分离):
 * 令牌走独立请求头(默认 admin-authentication), 由 JwtAdminInterceptor 校验;
 * 本控制器两个端点在管理端拦截器 exclude 列表中(登录本身不能要求已登录)
 */
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    /**
     * 管理端登录: 仅 user_type=1 账号可登录(种子账号见 sql/08, 上线必改密码)
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        return Result.success(adminAuthService.adminLogin(loginDTO));
    }

    /**
     * 管理端登出: 令牌进黑名单至自然过期
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        return adminAuthService.adminLogout(request);
    }
}

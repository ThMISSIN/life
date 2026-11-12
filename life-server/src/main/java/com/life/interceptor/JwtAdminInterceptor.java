package com.life.interceptor;

import com.life.constant.JwtClaimsConstant;
import com.life.constant.RedisConstant;
import com.life.constant.UserTypeConstant;
import com.life.context.BaseContext;
import com.life.properties.JwtProperties;
import com.life.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 2.3 管理端令牌拦截器(/admin/**, 上传通道除外):
 * 独立密钥(adminSecretKey) + 独立请求头(adminTokenName) + userType=ADMIN 声明三重校验——
 * C端令牌(用户密钥签发)在本拦截器解析必失败, 普通用户即使拿到管理端接口路径也无法越权;
 * 管理端令牌反向调C端接口同样因密钥不同被 JwtUserInterceptor 拒绝, 双端令牌互不通用
 */
@Slf4j
public class JwtAdminInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;

    public JwtAdminInterceptor(JwtProperties jwtProperties, StringRedisTemplate stringRedisTemplate) {
        this.jwtProperties = jwtProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String token = request.getHeader(adminTokenName());
        if (token != null && !token.isEmpty()
                && !Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConstant.USER_TOKEN_BLACKLIST + token))) {
            try {
                Claims claims = JwtUtil.parseJWT(adminSecret(), token);
                Object userType = claims.get(JwtClaimsConstant.USER_TYPE);
                Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
                // 声明级校验: 必须是管理员令牌(防普通用户令牌换头重放)
                if (userType != null && Integer.parseInt(userType.toString()) == UserTypeConstant.ADMIN) {
                    BaseContext.setCurrentId(userId);
                    return true;
                }
                log.warn("非管理员令牌访问管理端接口被拒 userId={} uri={}", userId, request.getRequestURI());
            } catch (Exception ex) {
                log.debug("管理端jwt校验失败: {}", ex.getMessage());
            }
        }
        // 无有效管理端身份: 403(与C端401区分, 前端不触发"重新登录"跳转)
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":0,\"msg\":\"无管理端权限\"}");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }

    /** 未配置独立密钥时回退C端密钥(userType声明校验仍生效, 部署时应配置 life.jwt.admin-secret-key) */
    private String adminSecret() {
        String s = jwtProperties.getAdminSecretKey();
        return s == null || s.isBlank() ? jwtProperties.getUserSecretKey() : s;
    }

    private String adminTokenName() {
        String n = jwtProperties.getAdminTokenName();
        return n == null || n.isBlank() ? "admin-authentication" : n;
    }
}

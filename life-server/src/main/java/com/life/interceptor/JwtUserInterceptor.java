package com.life.interceptor;

import com.life.constant.JwtClaimsConstant;
import com.life.constant.RedisKeyConstant;
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
 * jwt令牌拦截器（单类双实例）
 * requireLogin=true：必登录接口，游客或无效令牌直接401
 * requireLogin=false：可选登录接口，带有效令牌则解析用户身份，否则按匿名放行
 */
@Slf4j
public class JwtUserInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    private final StringRedisTemplate stringRedisTemplate;

    private final boolean requireLogin;

    public JwtUserInterceptor(JwtProperties jwtProperties, StringRedisTemplate stringRedisTemplate, boolean requireLogin) {
        this.jwtProperties = jwtProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.requireLogin = requireLogin;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName());

        //2、有令牌则尝试解析：校验登出黑名单后解析JWT，写入用户身份
        if (token != null && !token.isEmpty()) {
            Boolean isBlacklisted = stringRedisTemplate.hasKey(RedisKeyConstant.USER_TOKEN_BLACKLIST + token);
            if (!Boolean.TRUE.equals(isBlacklisted)) {
                try {
                    Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
                    Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
                    BaseContext.setCurrentId(userId);
                    //3、解析成功，放行
                    return true;
                } catch (Exception ex) {
                    //令牌无效，落到下面按requireLogin处理
                    log.debug("jwt校验失败：{}", ex.getMessage());
                }
            }
        }

        //4、无有效身份：必登录接口401拦截，可选登录接口按匿名放行
        if (requireLogin) {
            response.setStatus(401);
            return false;
        }
        return true;
    }

    /**
     * 请求完成后清理ThreadLocal，防止线程池中用户身份串
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}

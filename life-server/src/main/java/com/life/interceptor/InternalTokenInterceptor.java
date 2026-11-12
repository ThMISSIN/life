package com.life.interceptor;

import com.life.context.BaseContext;
import com.life.properties.AgentProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 内部服务调用拦截器（/ai-tools/** 专用，Python Agent 工具网关）。
 *
 * 信任链：Python 侧已完成终端用户 JWT 校验并回源 Java 查询，
 * 本拦截器只校验共享内部令牌，并把 Java 注入的 X-User-Id 写入
 * BaseContext，使下游 Service 的"从登录态取 userId"逻辑原样生效。
 * 用户身份缺失（anonymous）直接拒绝——业务工具全部是用户维度能力。
 */
@Slf4j
@RequiredArgsConstructor
public class InternalTokenInterceptor implements HandlerInterceptor {

    private final AgentProperties agentProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 1、内部令牌校验（配置为空 = 本地开发不校验，与 Python 侧策略一致）
        String expected = agentProperties.getInternalToken();
        if (expected != null && !expected.isEmpty()) {
            String token = request.getHeader("X-Agent-Token");
            if (!expected.equals(token)) {
                response.setStatus(401);
                return false;
            }
        }

        // 2、用户身份注入：必须是数字 userId，anonymous/缺失一律拒绝
        String uid = request.getHeader("X-User-Id");
        if (uid == null || uid.isBlank() || "anonymous".equalsIgnoreCase(uid.trim())) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"缺少用户身份\"}");
            return false;
        }
        try {
            BaseContext.setCurrentId(Long.valueOf(uid.trim()));
        } catch (NumberFormatException e) {
            response.setStatus(401);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}

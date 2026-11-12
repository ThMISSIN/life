package com.life.interceptor;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.life.context.BaseContext;
import com.life.properties.SentinelProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 2.10 接口层 Sentinel 限流埋点: 资源名=URL pattern(与 life.sentinel.rules 配置键一致),
 * 热点参数=userId(单用户QPS) + 资源级总QPS 双规则(见 SentinelRuleConfig);
 * 注册顺序须在 JwtUserInterceptor 之后(BaseContext 已有身份); 触发限流返回 429
 */
@Slf4j
public class SentinelLimitInterceptor implements HandlerInterceptor {

    private final SentinelProperties properties;

    public SentinelLimitInterceptor(SentinelProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.isEnabled() || properties.getRules().isEmpty()) {
            return true;
        }
        String resource = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (resource == null || !properties.getRules().containsKey(resource)) {
            return true;
        }
        Long userId = BaseContext.getCurrentId();
        Entry entry = null;
        try {
            if (userId != null) {
                entry = SphU.entry(resource, EntryType.IN, 1, userId);
            } else {
                entry = SphU.entry(resource, EntryType.IN);
            }
            return true;
        } catch (BlockException e) {
            log.warn("Sentinel 限流触发 resource={} userId={}", resource, userId);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"请求过于频繁, 请稍后再试\"}");
            return false;
        } finally {
            if (entry != null) {
                if (userId != null) {
                    entry.exit(1, userId);
                } else {
                    entry.exit();
                }
            }
        }
    }
}

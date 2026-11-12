package com.life.interceptor;

import com.life.mq.TraceIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 4.10 链路追踪: 每请求生成/透传 traceId(X-Trace-Id 头, 网关/前端可续传),
 * 写入 MDC 供日志 pattern %X{traceId} 输出, 并回写响应头方便排障对账;
 * MQ 透传见 MqProducer/MqTraceSupport, 定时任务见 TaskLockSupport
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TraceIdHolder.TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(TraceIdHolder.TRACE_ID, traceId);
        response.setHeader(TraceIdHolder.TRACE_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIdHolder.TRACE_ID);
        }
    }
}

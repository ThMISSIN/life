package com.life.mq;

/**
 * 链路追踪常量(4.10): MDC key 与透传头统一在此定义
 */
public class TraceIdHolder {

    /** MDC key(日志 pattern %X{traceId} 引用) */
    public static final String TRACE_ID = "traceId";

    /** HTTP 请求/响应头名 */
    public static final String TRACE_HEADER = "X-Trace-Id";
}

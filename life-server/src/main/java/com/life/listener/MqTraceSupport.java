package com.life.listener;

import com.life.constant.MqConstant;
import com.life.mq.TraceIdHolder;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;

/**
 * 监听器公共辅助(2.12/4.10): 从消息头恢复 traceId 到 MDC, 日志链路贯穿 MQ
 */
public final class MqTraceSupport {

    private MqTraceSupport() {
    }

    public static void restore(Message raw) {
        Object traceId = raw.getMessageProperties().getHeader(MqConstant.HEADER_TRACE_ID);
        if (traceId != null) {
            MDC.put(TraceIdHolder.TRACE_ID, String.valueOf(traceId));
        }
    }
}

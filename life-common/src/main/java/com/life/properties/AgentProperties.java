package com.life.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python AI Agent 伴生服务配置（life-agent FastAPI）
 * 信任模型：本服务是唯一受信调用方，内部令牌随每个请求透传
 */
@Component
@ConfigurationProperties(prefix = "life.agent")
@Data
public class AgentProperties {

    /** 是否启用 AI 能力（false 时接口返回业务提示，不发起远程调用） */
    private boolean enabled = true;

    /** Python Agent 服务地址（含 API 前缀），如 http://127.0.0.1:8000/api/v1 */
    private String baseUrl = "http://127.0.0.1:8000/api/v1";

    /** 内部共享令牌，需与 Python 侧 AGENT_INTERNAL_TOKEN 一致 */
    private String internalToken = "";

    /** 连接超时（秒） */
    private int connectTimeoutSeconds = 5;

    /** 读超时（秒）：ReAct 多步+工具+反思的整链预算 */
    private int readTimeoutSeconds = 120;
}

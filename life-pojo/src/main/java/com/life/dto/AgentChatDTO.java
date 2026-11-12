package com.life.dto;

import lombok.Data;

/**
 * AI 对话请求（/ai/chat 与 /ai/chat/stream 共用）
 */
@Data
public class AgentChatDTO {

    /** 用户问题 */
    private String question;

    /**
     * 会话主题 ID（可选）：用于同一用户开多个话题。
     * 服务端最终会话键为 user:{userId} 或 user:{userId}:{sessionId}，
     * 用户前缀强制注入，前端无法越权读到他人会话记忆。
     */
    private String sessionId;

    /** 编排模式：react(默认) | plan_execute */
    private String mode;

    /** 低置信度时是否跳过澄清直接作答（默认先澄清） */
    private Boolean forceAnswer;
}

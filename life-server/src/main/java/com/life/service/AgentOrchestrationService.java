package com.life.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.life.client.AgentClient;
import com.life.dto.AgentChatDTO;
import com.life.json.JacksonObjectMapper;
import com.life.properties.AgentProperties;
import com.life.result.Result;
import com.life.util.MultipartBuilder;
import com.life.vo.AgentAnswerVO;
import com.life.vo.AgentDocVO;
import com.life.vo.AgentSearchVO;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI Agent 编排服务：对 Java 上层屏蔽 Python 细节，强制用户级会话隔离。
 * 远程响应解析统一用 Jackson（@JsonAlias 兼容 Python snake_case）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrationService {

    private final AgentClient client;
    private final AgentProperties props;

    // 复用项目的 Jackson 配置（未知属性忽略），仅用于解析 Python 侧返回
    private static final ObjectMapper MAPPER = new JacksonObjectMapper();

    /**
     * 会话键：始终加 user:{userId} 前缀，防止前端传入他人 sessionId 越权读取记忆。
     * sessionId 为空 → user:{userId}；非空 → user:{userId}:{sessionId}。
     */
    private String scopeSession(String rawSession, Long userId) {
        String base = "user:" + userId;
        if (rawSession == null || rawSession.isBlank()) {
            return base;
        }
        // 仅保留安全字符，防止 Header/路径注入；再截断到 64 位
        String safe = rawSession.replaceAll("[^A-Za-z0-9_\\-]", "");
        if (safe.length() > 64) {
            safe = safe.substring(0, 64);
        }
        return safe.isEmpty() ? base : base + ":" + safe;
    }

    public Result<AgentAnswerVO> chat(AgentChatDTO dto, Long userId) {
        if (!props.isEnabled()) {
            return Result.error("AI 能力当前未启用");
        }
        if (dto.getQuestion() == null || dto.getQuestion().isBlank()) {
            return Result.error("问题不能为空");
        }
        try {
            String body = buildChatBody(dto, userId);
            String json = client.postJson("/agent/chat", body, userId);
            AgentAnswerVO vo = MAPPER.readValue(json, AgentAnswerVO.class);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("AI 对话失败 userId={}: {}", userId, e.getMessage());
            return Result.error("AI 服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 流式对话：把 Python SSE 的每条 data 帧解析后回调 onEvent（事件对象含 type/data）。
     * 上层的 SseEmitter 转发在 Controller 完成，此处只负责拉流解析。
     */
    public void streamChat(AgentChatDTO dto, Long userId, Consumer<JsonNode> onEvent) throws Exception {
        if (!props.isEnabled()) {
            throw new IllegalStateException("AI 能力当前未启用");
        }
        if (dto.getQuestion() == null || dto.getQuestion().isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        String body = buildChatBody(dto, userId);
        client.postSse("/agent/chat/stream", body, userId, line -> {
            // line 形如 "data: {json}"
            String payload = line.startsWith("data:") ? line.substring(5).trim() : line.trim();
            if (payload.isEmpty()) {
                return;
            }
            try {
                onEvent.accept(MAPPER.readTree(payload));
            } catch (Exception ex) {
                log.warn("SSE 帧解析失败，跳过: {}", payload);
            }
        });
    }

    public Result<AgentDocVO> uploadKnowledge(String filename, String contentType, byte[] data, Long userId) {
        if (!props.isEnabled()) {
            return Result.error("AI 能力当前未启用");
        }
        try {
            MultipartBuilder mp = MultipartBuilder.create()
                    .addFile("file", filename, contentType, data);
            String json = client.postMultipart(
                    "/agent/knowledge/documents", mp.contentType(), mp.bodyBytes(), userId);
            AgentDocVO vo = MAPPER.readValue(json, AgentDocVO.class);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("知识入库失败 userId={}: {}", userId, e.getMessage());
            return Result.error("知识入库失败：" + rootMsg(e));
        }
    }

    public Result<AgentSearchVO> search(String query, int topK, Long userId) {
        if (!props.isEnabled()) {
            return Result.error("AI 能力当前未启用");
        }
        try {
            String q = java.net.URLEncoder.encode(query, "UTF-8");
            String json = client.getJson("/agent/knowledge/search?query=" + q + "&top_k=" + topK, userId);
            AgentSearchVO vo = MAPPER.readValue(json, AgentSearchVO.class);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("知识检索失败: {}", e.getMessage());
            return Result.error("知识检索失败");
        }
    }

    public JsonNode status(Long userId) {
        try {
            return MAPPER.readTree(client.getJson("/agent/status", userId));
        } catch (Exception e) {
            log.warn("Agent 探活失败: {}", e.getMessage());
            return MAPPER.createObjectNode().put("ready", false).put("reason", String.valueOf(e.getMessage()));
        }
    }

    private String buildChatBody(AgentChatDTO dto, Long userId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("question", dto.getQuestion());
        m.put("session_id", scopeSession(dto.getSessionId(), userId));
        m.put("mode", dto.getMode() == null || dto.getMode().isBlank() ? "react" : dto.getMode());
        m.put("force_answer", Boolean.TRUE.equals(dto.getForceAnswer()));
        return JSONUtil.toJsonStr(m);
    }

    private String rootMsg(Throwable e) {
        Throwable c = e;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        String m = c.getMessage();
        return m == null ? e.toString() : (m.length() > 200 ? m.substring(0, 200) : m);
    }
}

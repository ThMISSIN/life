package com.life.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.life.context.BaseContext;
import com.life.dto.AgentChatDTO;
import com.life.result.Result;
import com.life.service.AgentOrchestrationService;
import com.life.vo.AgentAnswerVO;
import com.life.vo.AgentDocVO;
import com.life.vo.AgentSearchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI Agent 接口（均需登录，/ai/** 已注册拦截器）。
 * Java 只做鉴权、会话隔离、请求转发与流式桥接，AI 逻辑在 Python Agent。
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AgentOrchestrationService agentService;

    // 流式转发独立小线程池：Python SSE 拉流是阻塞 IO，不能占用 Tomcat 请求线程
    private final ExecutorService ssePool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "agent-sse-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });

    /**
     * 非流式编排对话。返回答案 + 意图/模式/轨迹/反思元信息。
     */
    @PostMapping("/chat")
    public Result<AgentAnswerVO> chat(@RequestBody AgentChatDTO dto) {
        return agentService.chat(dto, BaseContext.getCurrentId());
    }

    /**
     * 流式编排对话（SSE 桥接）：透传 Python 的 accepted/step/answer/error/done 事件。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AgentChatDTO dto) {
        final Long userId = BaseContext.getCurrentId();
        SseEmitter emitter = new SseEmitter(180_000L);

        ssePool.submit(() -> {
            try {
                agentService.streamChat(dto, userId, event -> {
                    try {
                        String type = event.path("type").asText("message");
                        emitter.send(SseEmitter.event().name(type).data(event.toString()));
                    } catch (IOException io) {
                        // 客户端断开，向上抛以中断拉流
                        throw new IllegalStateException("SSE 客户端已断开", io);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.warn("流式对话中断: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"message\":\"AI 服务中断\"}"));
                } catch (Exception ignore) {
                    // 收尾失败不再处理
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    /**
     * 知识文档入库：上传文件转发 Python ETL（解析→分块→嵌入→Milvus+BM25）。
     */
    @PostMapping("/knowledge/documents")
    public Result<AgentDocVO> uploadKnowledge(@RequestParam("file") MultipartFile file) {
        try {
            return agentService.uploadKnowledge(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes(),
                    BaseContext.getCurrentId());
        } catch (Exception e) {
            log.error("读取上传文件失败: {}", e.getMessage());
            return Result.error("读取上传文件失败");
        }
    }

    /**
     * 知识库检索（搜索框/调试直连，绕过 Agent 循环）。
     */
    @GetMapping("/knowledge/search")
    public Result<AgentSearchVO> search(@RequestParam String query,
                                        @RequestParam(defaultValue = "5") int topK) {
        return agentService.search(query, topK, BaseContext.getCurrentId());
    }

    /**
     * Agent 能力探活透传（前端据此显隐知识库入口）。
     */
    @GetMapping("/status")
    public Result<JsonNode> status() {
        return Result.success(agentService.status(BaseContext.getCurrentId()));
    }
}

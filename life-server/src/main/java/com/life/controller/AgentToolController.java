package com.life.controller;

import com.life.result.Result;
import com.life.service.AgentToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Agent 工具网关（内部接口，/ai-tools/** 由 InternalTokenInterceptor 保护，
 * 不走 JWT：调用方是 Python Agent，用户身份经 X-User-Id 注入 BaseContext）。
 *
 * 统一入口 POST /ai-tools/{toolName}，body 为 LLM 生成的 JSON 参数；
 * 具体白名单与参数护栏见 AgentToolService。
 */
@RestController
@RequestMapping("/ai-tools")
@RequiredArgsConstructor
@Slf4j
public class AgentToolController {

    private final AgentToolService agentToolService;

    @PostMapping("/{name}")
    public Result<Object> invoke(@PathVariable("name") String name,
                                 @RequestBody(required = false) Map<String, Object> args) {
        return agentToolService.invoke(name, args);
    }
}

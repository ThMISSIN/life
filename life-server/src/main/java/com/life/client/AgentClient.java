package com.life.client;

import com.life.properties.AgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Python Agent 服务 HTTP 客户端。
 * 统一注入内部令牌与用户身份头；提供 JSON POST 与 SSE 流式两种调用。
 * 用 JDK 内置 HttpClient，避免引入新依赖。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentClient {

    private final AgentProperties props;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    /**
     * JSON POST：返回响应体字符串。非 2xx 抛 IllegalStateException（含响应体）。
     *
     * @param path   相对路径（不含 base-url），如 /agent/chat
     * @param json   请求体 JSON
     * @param userId 用户身份（写入 X-User-Id 头），可为 null
     */
    public String postJson(String path, String json, Long userId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl() + path))
                .timeout(Duration.ofSeconds(props.getReadTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("X-Agent-Token", props.getInternalToken())
                .header("X-User-Id", userId == null ? "" : String.valueOf(userId))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("Agent 服务响应异常 status=" + resp.statusCode()
                    + " body=" + resp.body());
        }
        return resp.body();
    }

    /**
     * 原始 multipart POST（文件转发）。返回响应体字符串。
     *
     * @param contentType 完整 Content-Type（含服务端生成的 boundary），如 multipart/form-data; boundary=xxx
     * @param bodyBytes   已构造好的 multipart 请求体字节
     */
    public String postMultipart(String path, String contentType, byte[] bodyBytes, Long userId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl() + path))
                .timeout(Duration.ofSeconds(props.getReadTimeoutSeconds()))
                .header("Content-Type", contentType)
                .header("X-Agent-Token", props.getInternalToken())
                .header("X-User-Id", userId == null ? "" : String.valueOf(userId))
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("知识入库失败 status=" + resp.statusCode() + " body=" + resp.body());
        }
        return resp.body();
    }

    /**
     * GET JSON（探活/检索）。
     */
    public String getJson(String path, Long userId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl() + path))
                .timeout(Duration.ofSeconds(props.getReadTimeoutSeconds()))
                .header("X-Agent-Token", props.getInternalToken())
                .header("X-User-Id", userId == null ? "" : String.valueOf(userId))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("Agent 服务响应异常 status=" + resp.statusCode() + " body=" + resp.body());
        }
        return resp.body();
    }

    /**
     * JSON POST 并以 SSE 行流回调（用于流式对话转发）。
     * 每收到一行 "data: {...}" 就回调 onLine（含 data: 前缀，由上层解析）；
     * 连接关闭或异常由 onLine 正常返回/抛出决定上层收尾。
     */
    public void postSse(String path, String json, Long userId, Consumer<String> onLine) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl() + path))
                .timeout(Duration.ofSeconds(props.getReadTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("X-Agent-Token", props.getInternalToken())
                .header("X-User-Id", userId == null ? "" : String.valueOf(userId))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<java.io.InputStream> resp = httpClient.send(
                req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() / 100 != 2) {
            String err = readAll(resp.body());
            throw new IllegalStateException("流式 Agent 响应异常 status=" + resp.statusCode() + " body=" + err);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    onLine.accept(line);
                }
            }
        }
    }

    private String readAll(java.io.InputStream in) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return r.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "";
        }
    }
}

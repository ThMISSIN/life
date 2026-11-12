package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.life.constant.PostStatusConstant;
import com.life.entity.LifePost;
import com.life.json.JacksonObjectMapper;
import com.life.mapper.LifePostMapper;
import com.life.properties.EsProperties;
import com.life.service.PostSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2.11 ES 检索实现(Spring RestClient 直连 REST API, 无客户端版本绑定)
 * <p>降级链路: available()=false → PostServiceImpl 关键词搜索回退 SQL LIKE;
 * 双写失败仅告警(索引漂移由 reindexAll 全量重建校正); 探测结果缓存30s防雪崩式重试
 */
@Service
@Slf4j
public class PostSearchServiceImpl implements PostSearchService {

    private static final ObjectMapper MAPPER = new JacksonObjectMapper();
    private static final DateTimeFormatter ES_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private EsProperties properties;
    @Autowired
    private LifePostMapper postMapper;

    private volatile RestClient restClient;
    private volatile long lastProbeTime = 0;
    private volatile boolean lastProbeResult = false;
    private volatile boolean indexReady = false;

    @EventListener(ApplicationReadyEvent.class)
    public void initAsync() {
        Thread t = new Thread(() -> {
            if (properties.isEnabled() && available()) {
                ensureIndex();
            } else {
                log.info("ES 未启用或不可达, 关键词搜索走 SQL LIKE 降级路径");
            }
        }, "es-init");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public boolean available() {
        if (!properties.isEnabled()) {
            return false;
        }
        long now = System.currentTimeMillis();
        // 30s 探测缓存: 不可用时避免每请求都打一次超时探测
        if (now - lastProbeTime < 30_000) {
            return lastProbeResult;
        }
        synchronized (this) {
            if (System.currentTimeMillis() - lastProbeTime < 30_000) {
                return lastProbeResult;
            }
            try {
                client().get().uri("/").retrieve().toBodilessEntity();
                lastProbeResult = true;
            } catch (Exception e) {
                if (lastProbeResult) {
                    log.warn("ES 探测失败, 检索降级 SQL LIKE: {}", e.getMessage());
                }
                lastProbeResult = false;
            }
            lastProbeTime = System.currentTimeMillis();
            return lastProbeResult;
        }
    }

    @Override
    public void ensureIndex() {
        String index = properties.getPostIndex();
        try {
            // HEAD 判存在(存在则跳过, 不改既有mapping)
            try {
                client().head().uri("/{index}", index).retrieve().toBodilessEntity();
                indexReady = true;
                return;
            } catch (Exception notFound) {
                // 404 → 建索引
            }
            String mapping = buildMapping("ik_max_word", "ik_smart");
            try {
                client().put().uri("/{index}", index)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapping).retrieve().toBodilessEntity();
                log.info("ES 索引 {} 创建成功(ik分词)", index);
            } catch (Exception ikMissing) {
                // ik 插件缺失 → standard 分词兜底(中文按字切分, 可用但召回粒度粗)
                log.warn("ik 分词器不可用, 回退 standard 分词: {}", ikMissing.getMessage());
                client().put().uri("/{index}", index)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(buildMapping("standard", "standard")).retrieve().toBodilessEntity();
            }
            indexReady = true;
        } catch (Exception e) {
            log.warn("ES 索引创建失败(检索降级)", e);
        }
    }

    private String buildMapping(String indexAnalyzer, String searchAnalyzer) {
        return """
                {
                  "settings": { "number_of_shards": 1, "number_of_replicas": 0 },
                  "mappings": {
                    "properties": {
                      "title":      { "type": "text", "analyzer": "%s", "search_analyzer": "%s" },
                      "content":    { "type": "text", "analyzer": "%s", "search_analyzer": "%s" },
                      "userId":     { "type": "long" },
                      "category":   { "type": "keyword" },
                      "status":     { "type": "integer" },
                      "hotScore":   { "type": "double" },
                      "createTime": { "type": "date", "format": "yyyy-MM-dd HH:mm:ss||epoch_millis" }
                    }
                  }
                }
                """.formatted(indexAnalyzer, searchAnalyzer, indexAnalyzer, searchAnalyzer);
    }

    @Override
    public SearchResult search(String keyword, int pageNum, int pageSize) {
        if (!available()) {
            return null;
        }
        if (!indexReady) {
            // ES 晚于应用启动/索引被删: 惰性补建(HEAD判存在, 幂等轻量)
            ensureIndex();
        }
        if (!indexReady) {
            return null;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("query", Map.of("bool", Map.of(
                    "must", List.of(Map.of("multi_match", Map.of(
                            "query", keyword,
                            "fields", List.of("title^2", "content")))),
                    "filter", List.of(Map.of("term", Map.of("status", PostStatusConstant.PUBLISHED))))));
            body.put("from", (pageNum - 1) * pageSize);
            body.put("size", pageSize);
            String json = client().post().uri("/{index}/_search", properties.getPostIndex())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(MAPPER.writeValueAsString(body))
                    .retrieve().body(String.class);
            JsonNode root = MAPPER.readTree(json);
            long total = root.path("hits").path("total").path("value").asLong(
                    root.path("hits").path("total").asLong(0));
            List<Long> ids = new ArrayList<>();
            for (JsonNode hit : root.path("hits").path("hits")) {
                try {
                    ids.add(Long.parseLong(hit.path("_id").asText()));
                } catch (NumberFormatException ignore) {
                    // 脏文档id跳过
                }
            }
            return new SearchResult(total, ids);
        } catch (Exception e) {
            log.warn("ES 搜索失败, 本次降级 SQL LIKE kw={}", keyword, e);
            return null;
        }
    }

    @Override
    public void indexPost(LifePost post) {
        if (!properties.isEnabled() || post == null || post.getId() == null) {
            return;
        }
        if (!available() || !indexReady) {
            return;
        }
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("title", post.getTitle() == null ? "" : post.getTitle());
            doc.put("content", post.getContent() == null ? "" : post.getContent());
            doc.put("userId", post.getUserId());
            doc.put("category", post.getCategory());
            doc.put("status", post.getStatus());
            doc.put("hotScore", post.getHotScore());
            doc.put("createTime", post.getCreateTime() == null ? null : post.getCreateTime().format(ES_DATE_FMT));
            client().put().uri("/{index}/_doc/{id}", properties.getPostIndex(), post.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(MAPPER.writeValueAsString(doc))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            // 双写失败不阻断业务, reindexAll 兜底校正
            log.warn("ES 索引写入失败 postId={}", post.getId(), e);
        }
    }

    @Override
    public void removePost(Long postId) {
        if (!properties.isEnabled() || postId == null || !available() || !indexReady) {
            return;
        }
        try {
            client().delete().uri("/{index}/_doc/{id}", properties.getPostIndex(), postId)
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("ES 索引删除失败 postId={}", postId, e);
        }
    }

    @Override
    public int reindexAll() {
        if (!available()) {
            throw new IllegalStateException("ES 不可用, 无法重建索引");
        }
        ensureIndex();
        int total = 0;
        long lastId = 0;
        while (true) {
            List<LifePost> batch = postMapper.selectList(new LambdaQueryWrapper<LifePost>()
                    .in(LifePost::getStatus, PostStatusConstant.PUBLISHED, PostStatusConstant.TOP)
                    .gt(LifePost::getId, lastId)
                    .orderByAsc(LifePost::getId)
                    .last("LIMIT 500"));
            if (batch.isEmpty()) {
                break;
            }
            bulkIndex(batch);
            total += batch.size();
            lastId = batch.get(batch.size() - 1).getId();
        }
        log.info("ES 全量重建完成: {} 篇帖子", total);
        return total;
    }

    /**
     * _bulk NDJSON 批量写入(单批500)
     */
    private void bulkIndex(List<LifePost> posts) {
        try {
            StringBuilder nd = new StringBuilder();
            for (LifePost p : posts) {
                nd.append("{\"index\":{\"_index\":\"").append(properties.getPostIndex())
                        .append("\",\"_id\":\"").append(p.getId()).append("\"}}\n");
                Map<String, Object> doc = new HashMap<>();
                doc.put("title", p.getTitle() == null ? "" : p.getTitle());
                doc.put("content", p.getContent() == null ? "" : p.getContent());
                doc.put("userId", p.getUserId());
                doc.put("category", p.getCategory());
                doc.put("status", p.getStatus());
                doc.put("hotScore", p.getHotScore());
                doc.put("createTime", p.getCreateTime() == null ? null : p.getCreateTime().format(ES_DATE_FMT));
                nd.append(MAPPER.writeValueAsString(doc)).append('\n');
            }
            client().post().uri("/_bulk")
                    .contentType(MediaType.parseMediaType("application/x-ndjson"))
                    .body(nd.toString())
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("ES 批量写入失败 size={}", posts.size(), e);
        }
    }

    private RestClient client() {
        RestClient c = restClient;
        if (c == null) {
            synchronized (this) {
                if (restClient == null) {
                    var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(Duration.ofMillis(properties.getTimeoutMs()));
                    factory.setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()));
                    restClient = RestClient.builder()
                            .baseUrl(properties.getUri())
                            .requestFactory(factory)
                            .build();
                }
                c = restClient;
            }
        }
        return c;
    }
}

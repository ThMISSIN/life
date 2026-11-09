package com.life.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.life.constant.RedisConstant;
import com.life.entity.LifePost;
import com.life.json.JacksonObjectMapper;
import com.life.properties.PostHotProperties;
import com.life.service.PostFeedCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 2.11 推荐流多级缓存实现
 * <p>L1 Caffeine: 进程内热点页, 短TTL+容量上限, 多实例间靠TTL收敛;
 * L2 Redis: 仅第一页(首页并发最高, 值带TTL自然过期, 结构变更即时SCAN删除);
 * 回源后同时写两层。缓存的是实体行快照(不含用户态), liked/favorited 由调用方组装时实时回填。
 * <p>一致性口径: 发帖/删帖/转发(影响页码内容)即时失效; 点赞/评论/阅读(只动计数字段)
 * 靠短TTL收敛——热度流本就是软态排序, 不为计数做全链路失效
 */
@Service
@Slf4j
public class PostFeedCacheServiceImpl implements PostFeedCacheService {

    /** 与Web层同款的日期格式, 保证LocalDateTime序列化/反序列化对称 */
    private static final ObjectMapper MAPPER = new JacksonObjectMapper();

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PostHotProperties properties;

    private Cache<String, FeedPageSnapshot> l1;

    @PostConstruct
    public void init() {
        PostHotProperties.Feed cfg = properties.getFeed();
        l1 = Caffeine.newBuilder()
                .maximumSize(cfg.getCaffeineMaxPages())
                .expireAfterWrite(Duration.ofSeconds(cfg.getCaffeineTtlSeconds()))
                .build();
    }

    @Override
    public FeedPageSnapshot loadHotFeedPage(int pageNum, int pageSize, Supplier<Page<LifePost>> dbLoader) {
        PostHotProperties.Feed cfg = properties.getFeed();
        if (!cfg.isCacheEnabled()) {
            return fromPage(dbLoader.get());
        }
        String l1Key = "p" + pageNum + ":s" + pageSize;
        FeedPageSnapshot snapshot = l1.getIfPresent(l1Key);
        if (snapshot != null) {
            return snapshot;
        }
        // L2 只放首页: 第一页承载绝大多数读流量
        String l2Key = pageNum == 1 ? firstPageKey(pageSize) : null;
        if (l2Key != null) {
            snapshot = readL2(l2Key);
            if (snapshot != null) {
                l1.put(l1Key, snapshot);
                return snapshot;
            }
        }
        snapshot = fromPage(dbLoader.get());
        l1.put(l1Key, snapshot);
        if (l2Key != null) {
            writeL2(l2Key, snapshot, cfg.getRedisTtlSeconds());
        }
        return snapshot;
    }

    @Override
    public void evictHotFeed() {
        l1.invalidateAll();
        try {
            // SCAN(非KEYS)按前缀删首页键, 口径与 LikeServiceImpl.saveLikeRecord 一致
            Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> found = new HashSet<>();
                try (var cursor = connection.scan(ScanOptions.scanOptions()
                        .match(RedisConstant.POST_FEED_FIRST_PAGE_PREFIX + "*").count(100).build())) {
                    while (cursor.hasNext()) {
                        found.add(new String(cursor.next()));
                    }
                }
                return found;
            });
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 失效失败不阻塞发帖/删帖主流程, 首页最多脏一个TTL(默认30s)
            log.warn("推荐流首页缓存失效失败, 依赖TTL收敛", e);
        }
    }

    private String firstPageKey(int pageSize) {
        return RedisConstant.POST_FEED_FIRST_PAGE.replace("{pageSize}", String.valueOf(pageSize));
    }

    private FeedPageSnapshot fromPage(Page<LifePost> page) {
        return new FeedPageSnapshot(page.getTotal(), page.getRecords());
    }

    private FeedPageSnapshot readL2(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return MAPPER.readValue(json, FeedPageSnapshot.class);
        } catch (Exception e) {
            // 脏缓存/序列化版本变更: 删键回源, 不让缓存故障放大为500
            log.warn("推荐流L2缓存读取异常, 回源DB key={}", key, e);
            redisTemplate.delete(key);
            return null;
        }
    }

    private void writeL2(String key, FeedPageSnapshot snapshot, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, MAPPER.writeValueAsString(snapshot),
                    Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("推荐流L2缓存写入失败(仅失去缓存, 不影响本次响应)", e);
        }
    }
}

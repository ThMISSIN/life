package com.life.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifePost;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Supplier;

/**
 * 2.11 推荐流多级缓存: L1 Caffeine 本地热点页 → L2 Redis 首页第一页 → DB。
 * 只缓存"实体页"(帖子行+总数, 与登录用户无关); liked/favorited/followed 等
 * 用户态字段在缓存之后的组装环节实时回填, 避免串号
 */
public interface PostFeedCacheService {

    /**
     * 读取推荐流实体页(仅纯推荐流口径由调用方保证)
     *
     * @param dbLoader 缓存全miss时回源(由调用方构造查询分页)
     */
    FeedPageSnapshot loadHotFeedPage(int pageNum, int pageSize, Supplier<Page<LifePost>> dbLoader);

    /**
     * 推荐流结构变更(发帖/删帖/转发生成新行)时失效: L1全清 + L2首页键SCAN删除;
     * 互动计数(赞/评/阅)不做即时失效, 靠短TTL收敛
     */
    void evictHotFeed();

    /**
     * 缓存页快照(Jackson序列化进出Redis, LifePost含LocalDateTime走项目统一日期格式)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class FeedPageSnapshot {
        private long total;
        private List<LifePost> records;
    }
}

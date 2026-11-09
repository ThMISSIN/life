package com.life.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 2.11 热度推荐信息流参数(life.post.*)
 * <p>hot: 热度公式参数, SQL重算语句与布隆/缓存均以此为准;
 * 调整权重后无需手工刷数——衰减扫描任务会在窗口期内自然收敛
 * <p>公式: hot_score = 基础分(文字+图+标题) + 赞*w + 评*w + 转*w + 阅*w
 * + 新帖扶持(发布 newPostHours 内) - min(距今小时数*decayPerHour, decayMax)
 */
@Component
@ConfigurationProperties(prefix = "life.post")
@Data
public class PostHotProperties {

    private Hot hot = new Hot();

    private Feed feed = new Feed();

    private Bloom bloom = new Bloom();

    /** 热度公式参数 */
    @Data
    public static class Hot {
        /** 基础分: 文字帖保底 */
        private BigDecimal baseText = new BigDecimal("20");
        /** 基础分: 带图附加 */
        private BigDecimal baseImage = new BigDecimal("10");
        /** 基础分: 有标题附加 */
        private BigDecimal baseTitle = new BigDecimal("5");
        /** 互动权重: 单赞 */
        private BigDecimal weightLike = new BigDecimal("3");
        /** 互动权重: 单评 */
        private BigDecimal weightComment = new BigDecimal("2");
        /** 互动权重: 单转 */
        private BigDecimal weightRepost = new BigDecimal("4");
        /** 互动权重: 单阅(量大稀释) */
        private BigDecimal weightView = new BigDecimal("0.1");
        /** 新帖扶持分: 发布 new-post-hours 内额外加权, 防信息流沉底 */
        private BigDecimal newPostBoost = new BigDecimal("80");
        /** 新帖扶持时长(小时) */
        private int newPostHours = 24;
        /** 时间衰减: 每满1小时扣减 */
        private BigDecimal decayPerHour = new BigDecimal("0.5");
        /** 时间衰减封顶(超龄帖只靠互动翻红) */
        private BigDecimal decayMax = new BigDecimal("300");
        /** 衰减扫描窗口(小时): 只重算窗口内发布的帖, 扶持分随扫描自然摘除 */
        private int recalcWindowHours = 72;
        /** 衰减扫描单轮上限行数 */
        private int recalcBatchSize = 800;
    }

    /** 多级缓存参数 */
    @Data
    public static class Feed {
        /** 推荐流页缓存总开关(关闭则直查DB, 便于排障) */
        private boolean cacheEnabled = true;
        /** L2 Redis 首页缓存TTL(秒): 短TTL兜互动数新鲜度, 发帖/删帖/转发即时失效 */
        private long redisTtlSeconds = 30;
        /** L1 Caffeine 热点页TTL(秒): 多实例间靠短TTL收敛 */
        private long caffeineTtlSeconds = 10;
        /** L1 最多缓存页数 */
        private int caffeineMaxPages = 200;
        /** 浏览量回写任务单轮排空条目数 */
        private int viewDrainBatchSize = 50;
    }

    /** 布隆过滤器参数 */
    @Data
    public static class Bloom {
        /** 预计容纳帖子id数(初始化容量, 超出后误判率上升) */
        private long expectedIds = 1_000_000L;
        /** 期望误判率 */
        private double fpp = 0.01;
        /** 过滤器不存在时启动重建: 分页扫存量id灌入 */
        private boolean rebuildOnStart = true;
        /** 重建扫描分页大小 */
        private int rebuildBatchSize = 5000;
    }
}

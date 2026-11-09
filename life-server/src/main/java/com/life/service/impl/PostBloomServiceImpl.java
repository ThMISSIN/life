package com.life.service.impl;

import com.life.constant.RedisConstant;
import com.life.mapper.LifePostMapper;
import com.life.properties.PostHotProperties;
import com.life.service.PostBloomService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 2.11 帖子id布隆过滤器实现(Redisson RBloomFilter, 存Redis全实例共享)
 * <p>生命周期: 启动后异步 tryInit(容量/误判率) → 首次初始化成功才分页回灌存量id
 * (已存在的过滤器直接使用, 不重建——避免重灌窗口内的漏判); 发帖/转发实时 add。
 * <p>异常口径: Redis 不可用/未就绪一律 fail-open 返回"可能存在", 退回DB判定,
 * 绝不因过滤器故障误杀正常流量。
 */
@Service
@Slf4j
public class PostBloomServiceImpl implements PostBloomService {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private PostHotProperties properties;
    @Autowired
    private LifePostMapper postMapper;

    /** 就绪前为null: mightContain fail-open, add 静默跳过(存量由启动回灌兜底) */
    private volatile RBloomFilter<Long> filter;

    @EventListener(ApplicationReadyEvent.class)
    public void initAsync() {
        // 守护线程异步初始化, 存量再大也不拖启动; Redis暂时不可用则15s退避重试直到就绪,
        // 期间 mayContain fail-open / add 跳过(新帖由回灌扫描兜住, 已落库)
        Thread t = new Thread(() -> {
            while (this.filter == null) {
                if (ensureFilter() != null) {
                    break;
                }
                try {
                    Thread.sleep(15_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "post-bloom-init");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public boolean mightContain(Long postId) {
        RBloomFilter<Long> f = this.filter;
        if (f == null || postId == null) {
            return true;
        }
        try {
            return f.contains(postId);
        } catch (Exception e) {
            log.warn("布隆过滤器查询异常, fail-open postId={}", postId, e);
            return true;
        }
    }

    @Override
    public void add(Long postId) {
        if (postId == null) {
            return;
        }
        RBloomFilter<Long> f = this.filter;
        if (f == null) {
            // 启动初始化尚未完成: 不阻塞请求线程, 该帖已落库, 存量回灌扫描会带上它
            log.debug("布隆过滤器未就绪, 跳过登记 postId={}", postId);
            return;
        }
        try {
            f.add(postId);
        } catch (Exception e) {
            // 登记失败即漏id, 新帖在转发/阅读入口可能被误判不存在(布隆无假阴性);
            // 发帖已成功不回滚, 依赖Redis恢复后的下轮回灌/重启窗口自愈, 告警跟进
            log.error("布隆过滤器登记帖子id失败 postId={}", postId, e);
        }
    }

    /**
     * 懒确保过滤器可用: tryInit 幂等(已存在返回false不覆盖); 只有本次真正新建时才回灌存量
     */
    private RBloomFilter<Long> ensureFilter() {
        RBloomFilter<Long> f = this.filter;
        if (f != null) {
            return f;
        }
        synchronized (this) {
            if (this.filter != null) {
                return this.filter;
            }
            try {
                PostHotProperties.Bloom cfg = properties.getBloom();
                RBloomFilter<Long> bf = redissonClient.getBloomFilter(RedisConstant.POST_BLOOM);
                boolean fresh = bf.tryInit(cfg.getExpectedIds(), cfg.getFpp());
                this.filter = bf;
                if (fresh && cfg.isRebuildOnStart()) {
                    backfill(bf, cfg.getRebuildBatchSize());
                } else if (!fresh) {
                    log.info("帖子布隆过滤器已存在, 跳过重建 (误判率以初始化时容量为准)");
                }
                return bf;
            } catch (Exception e) {
                log.warn("帖子布隆过滤器初始化失败, 穿透防线降级为直接查库", e);
                return null;
            }
        }
    }

    /**
     * 存量回灌: 按id游标分页(含已删行——布隆不需要精确, 少灌只会多拦, 多灌只增误判率)
     */
    private void backfill(RBloomFilter<Long> bf, int batchSize) {
        long start = System.currentTimeMillis();
        long lastId = 0;
        long total = 0;
        while (true) {
            List<Long> ids = postMapper.selectIdsAfter(lastId, batchSize);
            if (ids.isEmpty()) {
                break;
            }
            bf.add(ids);
            total += ids.size();
            lastId = ids.get(ids.size() - 1);
            if (ids.size() < batchSize) {
                break;
            }
        }
        log.info("帖子布隆过滤器存量回灌完成: {} 个id, 耗时{}ms", total, System.currentTimeMillis() - start);
    }
}

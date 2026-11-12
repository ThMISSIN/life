package com.life.service.impl;

import com.life.constant.RedisConstant;
import com.life.entity.LifeBenefit;
import com.life.entity.LifeProduct;
import com.life.mapper.LifeBenefitMapper;
import com.life.mapper.LifeProductMapper;
import com.life.properties.StockProperties;
import com.life.service.StockCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 2.14 Redis+Lua 库存镜像实现
 * <p>Lua 单脚本原子完成 "存在性检查 + 余量校验 + DECRBY", 替代 setnx 锁逐 key 排队的热点争抢;
 * DB 条件扣减(stock>=n)仍保留为最终权威——Redis 只挡量不记账, 两层都过才算扣成功;
 * 漂移(补偿失败/管理端直改DB)由对账任务以 DB 为准周期性校正
 */
@Service
@Slf4j
public class StockCacheServiceImpl implements StockCacheService {

    /**
     * 扣减脚本: GET 判存在 → 余量校验 → DECRBY; 返回 -1 未预热 / -2 不足 / >=0 剩余
     */
    private static final DefaultRedisScript<Long> DEDUCT_SCRIPT = new DefaultRedisScript<>("""
            local v = redis.call('GET', KEYS[1])
            if v == false then return -1 end
            local stock = tonumber(v)
            local qty = tonumber(ARGV[1])
            if stock < qty then return -2 end
            return redis.call('DECRBY', KEYS[1], qty)
            """, Long.class);

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private StockProperties properties;
    @Autowired
    private LifeProductMapper productMapper;
    @Autowired
    private LifeBenefitMapper benefitMapper;

    @Override
    public long tryDeduct(String itemType, Long itemId, int quantity) {
        if (!properties.isEnabled() || itemId == null || quantity <= 0) {
            return MISS;
        }
        try {
            Long result = redisTemplate.execute(DEDUCT_SCRIPT, List.of(stockKey(itemType, itemId)),
                    String.valueOf(quantity));
            return result == null ? MISS : result;
        } catch (Exception e) {
            // Redis 故障降级纯 DB 路径(权威扣减仍在, 不放大故障)
            log.warn("库存Lua扣减异常, 降级DB路径 {}:{} ", itemType, itemId, e);
            return MISS;
        }
    }

    @Override
    public void rollback(String itemType, Long itemId, int quantity) {
        if (!properties.isEnabled() || itemId == null || quantity <= 0) {
            return;
        }
        try {
            String key = stockKey(itemType, itemId);
            // 仅镜像存在时回补, 避免给未预热商品凭空造出镜像
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.opsForValue().increment(key, quantity);
            }
        } catch (Exception e) {
            // 回补失败留给对账任务校正
            log.warn("库存镜像回补失败(对账兜底) {}:{}", itemType, itemId, e);
        }
    }

    @Override
    public boolean preheat(String itemType, Long itemId, int dbStock) {
        if (!properties.isEnabled() || itemId == null || dbStock < 0) {
            // 不限量(-1)/异常值不建镜像, 永远走DB路径
            return false;
        }
        try {
            String key = stockKey(itemType, itemId);
            Duration ttl = properties.getCacheTtlHours() > 0
                    ? Duration.ofHours(properties.getCacheTtlHours()) : Duration.ZERO;
            Boolean created = ttl.isZero()
                    ? redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(dbStock))
                    : redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(dbStock), ttl);
            if (Boolean.TRUE.equals(created)) {
                redisTemplate.opsForSet().add(RedisConstant.STOCK_PREHEATED, itemType + ":" + itemId);
                log.info("库存镜像预热 {}:{} stock={}", itemType, itemId, dbStock);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("库存镜像预热失败 {}:{} (走DB路径)", itemType, itemId, e);
            return false;
        }
    }

    @Override
    public int reconcile() {
        if (!properties.isEnabled()) {
            return 0;
        }
        int fixed = 0;
        try {
            Set<String> members = redisTemplate.opsForSet().members(RedisConstant.STOCK_PREHEATED);
            if (members == null || members.isEmpty()) {
                return 0;
            }
            for (String member : members) {
                String[] parts = member.split(":");
                if (parts.length != 2) {
                    redisTemplate.opsForSet().remove(RedisConstant.STOCK_PREHEATED, member);
                    continue;
                }
                String itemType = parts[0];
                Long itemId = Long.valueOf(parts[1]);
                Integer dbStock = loadDbStock(itemType, itemId);
                String key = stockKey(itemType, itemId);
                if (dbStock == null || dbStock < 0) {
                    // DB 已删/不限量: 摘除镜像与注册表
                    redisTemplate.delete(key);
                    redisTemplate.opsForSet().remove(RedisConstant.STOCK_PREHEATED, member);
                    fixed++;
                    continue;
                }
                String cached = redisTemplate.opsForValue().get(key);
                if (cached == null) {
                    // 镜像TTL到期: 注册表同步摘除(下次下单按需重新预热)
                    redisTemplate.opsForSet().remove(RedisConstant.STOCK_PREHEATED, member);
                    continue;
                }
                if (!cached.equals(String.valueOf(dbStock))) {
                    log.warn("[库存对账] 漂移校正 {}:{} redis={} -> db={}", itemType, itemId, cached, dbStock);
                    redisTemplate.opsForValue().set(key, String.valueOf(dbStock),
                            Duration.ofHours(Math.max(properties.getCacheTtlHours(), 1)));
                    fixed++;
                }
            }
        } catch (Exception e) {
            log.warn("库存对账任务异常(下轮重试)", e);
        }
        return fixed;
    }

    private Integer loadDbStock(String itemType, Long itemId) {
        if (TYPE_PRODUCT.equals(itemType)) {
            LifeProduct p = productMapper.selectById(itemId);
            return p == null ? null : p.getStock();
        }
        if (TYPE_BENEFIT.equals(itemType)) {
            LifeBenefit b = benefitMapper.selectById(itemId);
            return b == null ? null : b.getStock();
        }
        return null;
    }

    private String stockKey(String itemType, Long itemId) {
        return RedisConstant.STOCK_CACHE
                .replace("{itemType}", itemType)
                .replace("{itemId}", itemId.toString());
    }
}

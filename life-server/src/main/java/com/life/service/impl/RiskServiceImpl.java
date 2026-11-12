package com.life.service.impl;

import com.life.constant.RedisConstant;
import com.life.entity.LifeRiskRecord;
import com.life.exception.ValidationException;
import com.life.mapper.LifeRiskRecordMapper;
import com.life.properties.RiskProperties;
import com.life.service.RiskService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.UUID;

/**
 * 2.10 风控实现: Redis ZSet 滑动窗口(窗口外成员即清, ZCARD 计数) + 风险标记(短TTL自动解除)
 * <p>口径: 命中阈值当次即拦截并标记, 标记期内所有敏感动作(点赞/签到/领取/发帖/评论/下单/阅读)拒绝;
 * 标记自然过期即恢复(教育型风控, 无人工解封); 全程 fail-open —— Redis/DB 异常不阻断正常业务
 */
@Service
@Slf4j
public class RiskServiceImpl implements RiskService {

    /** 前端设备指纹请求头(2.10 采集层) */
    public static final String FINGERPRINT_HEADER = "X-Device-Fingerprint";

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RiskProperties properties;
    @Autowired
    private LifeRiskRecordMapper riskRecordMapper;

    @Override
    public void checkAndCollect(String action, Long userId) {
        if (!properties.isEnabled() || userId == null) {
            return;
        }
        // 1. 已标记风险: 直接拦截
        if (isRisky(userId)) {
            throw new ValidationException("操作过于频繁, 已被限制, 请稍后再试");
        }
        // 2. 滑动窗口计数(未配置窗口的动作只受标记拦截)
        RiskProperties.WindowRule rule = properties.getWindows().get(action);
        if (rule == null || rule.getLimit() <= 0) {
            return;
        }
        try {
            String key = RedisConstant.RISK_WINDOW
                    .replace("{action}", action)
                    .replace("{userId}", userId.toString());
            long now = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, now - rule.getSeconds() * 1000L);
            redisTemplate.expire(key, Duration.ofSeconds(rule.getSeconds() * 2L));
            Long count = redisTemplate.opsForZSet().zCard(key);
            if (count != null && count > rule.getLimit()) {
                markRisky(userId, action, rule);
                throw new ValidationException("操作过于频繁, 已被限制, 请稍后再试");
            }
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            // 风控自身故障 fail-open, 不影响主业务
            log.warn("风控滑动窗口评估异常, 放行 userId={} action={}", userId, action, e);
        }
    }

    @Override
    public boolean isRisky(Long userId) {
        if (!properties.isEnabled() || userId == null) {
            return false;
        }
        try {
            String flagKey = RedisConstant.RISK_FLAG.replace("{userId}", userId.toString());
            return Boolean.TRUE.equals(redisTemplate.hasKey(flagKey));
        } catch (Exception e) {
            log.warn("风控标记查询异常, fail-open userId={}", userId, e);
            return false;
        }
    }

    /**
     * 打风险标记(setnx 保证同一标记周期只落一条风控记录, 不被高频请求刷爆表)
     */
    private void markRisky(Long userId, String action, RiskProperties.WindowRule rule) {
        String flagKey = RedisConstant.RISK_FLAG.replace("{userId}", userId.toString());
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(flagKey, action, Duration.ofMinutes(properties.getFlagTtlMinutes()));
        String hitRule = action + " 超过 " + rule.getLimit() + "次/" + rule.getSeconds() + "s";
        log.warn("风控命中: userId={} {}", userId, hitRule);
        if (Boolean.TRUE.equals(first)) {
            try {
                riskRecordMapper.insert(LifeRiskRecord.builder()
                        .userId(userId)
                        .ip(currentIp())
                        .deviceFingerprint(currentFingerprint())
                        .actionType(action)
                        .riskLevel(2)
                        .hitRule(hitRule)
                        .build());
            } catch (Exception e) {
                log.warn("风控记录落库失败(标记已生效)", e);
            }
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private String currentIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String currentFingerprint() {
        HttpServletRequest req = currentRequest();
        return req == null ? null : req.getHeader(FINGERPRINT_HEADER);
    }
}

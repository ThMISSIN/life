package com.life.service.impl;

import com.life.constant.RedisConstant;
import com.life.constant.RiskActionConstant;
import com.life.mapper.LifePostMapper;
import com.life.properties.PostHotProperties;
import com.life.service.PostBloomService;
import com.life.service.PostHotService;
import com.life.service.RiskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 2.11 热度分实现: 公式全部收敛在 LifePostMapper.xml hotScoreExpr(单语句原子重算,
 * 计数取行内现值, 并发互动天然合并不丢失), 本类只做触发与参数供给
 */
@Service
@Slf4j
public class PostHotServiceImpl implements PostHotService {

    /** 阅读去重key的日期段格式: 同用户同帖每日只计1次 */
    private static final DateTimeFormatter VIEW_DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    @Autowired
    private LifePostMapper postMapper;
    @Autowired
    private PostHotProperties properties;
    @Autowired
    private PostBloomService bloomService;
    @Autowired
    private RiskService riskService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void recalc(Long postId) {
        if (postId == null) {
            return;
        }
        try {
            postMapper.recalcHotScore(postId, properties.getHot());
        } catch (Exception e) {
            // 热度是排序软态, 重算失败不影响主业务(点赞/评论/转发已落库), 下轮扫描兜底收敛
            log.warn("热度重算失败 postId={}", postId, e);
        }
    }

    @Override
    public void recalcBatch(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return;
        }
        try {
            postMapper.recalcHotScoreByIds(postIds, properties.getHot());
        } catch (Exception e) {
            log.warn("热度批量重算失败 count={}", postIds.size(), e);
        }
    }

    @Override
    public int sweepRecentPosts() {
        int rows = postMapper.recalcHotScoreRecent(properties.getHot());
        if (rows > 0) {
            log.debug("热度衰减扫描重算 {} 行", rows);
        }
        return rows;
    }

    @Override
    public boolean recordView(Long postId, Long userId) {
        if (postId == null || userId == null) {
            return false;
        }
        // 缓存穿透防线①: 布隆说"没有"就是没有, 不占去重名额也不打DB
        if (!bloomService.mightContain(postId)) {
            return false;
        }
        // 2.10 风控采集(阅读是软态统计, 被标记用户静默不计, 不抛错不打扰前端)
        try {
            riskService.checkAndCollect(RiskActionConstant.VIEW, userId);
        } catch (Exception e) {
            return false;
        }
        // 防刷: 同一用户同一帖当日只计1次(setnx到日切, 重进详情页不重复累加)
        String dedupKey = RedisConstant.POST_VIEWED_DEDUP
                .replace("{userId}", userId.toString())
                .replace("{postId}", postId.toString())
                .replace("{date}", LocalDate.now().format(VIEW_DATE_FMT));
        Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofDays(1));
        if (!Boolean.TRUE.equals(first)) {
            return false;
        }
        // ZINCRBY 累计增量, 排空回写见 drainViewTimes
        redisTemplate.opsForZSet().incrementScore(RedisConstant.POST_VIEW_TIMES, postId.toString(), 1);
        return true;
    }

    @Override
    public int drainViewTimes() {
        int batchSize = properties.getFeed().getViewDrainBatchSize();
        // 与点赞 LIKES_TIMES 同款排空口径: 从小到大取一批 → 按成员精确ZREM;
        // 取数与删除间隙的并发增量最多丢1次去重后的阅读, 属统计软态可接受
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().rangeWithScores(RedisConstant.POST_VIEW_TIMES, 0, batchSize - 1L);
        if (tuples == null || tuples.isEmpty()) {
            return 0;
        }
        List<String> members = new ArrayList<>(tuples.size());
        List<Long> touchedIds = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null) {
                continue;
            }
            members.add(tuple.getValue());
            long delta = Math.round(tuple.getScore() == null ? 0 : tuple.getScore());
            if (delta <= 0) {
                continue;
            }
            if (postMapper.incViewCount(Long.valueOf(tuple.getValue()), (int) Math.min(delta, Integer.MAX_VALUE)) > 0) {
                touchedIds.add(Long.valueOf(tuple.getValue()));
            }
        }
        if (!members.isEmpty()) {
            redisTemplate.opsForZSet().remove(RedisConstant.POST_VIEW_TIMES, members.toArray());
        }
        // "阅"权重即时反映到热度
        recalcBatch(touchedIds);
        return touchedIds.size();
    }
}

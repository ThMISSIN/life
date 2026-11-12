package com.life.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.BizTypeConstant;
import com.life.constant.PostStatusConstant;
import com.life.constant.RedisConstant;
import com.life.constant.RiskActionConstant;
import com.life.constant.TaskEventTypeConstant;
import com.life.context.BaseContext;
import com.life.dto.PostPublishDTO;
import com.life.entity.LifeFollow;
import com.life.entity.LifePost;
import com.life.entity.LifeUser;
import com.life.exception.BaseException;
import com.life.exception.UserNotLoginException;
import com.life.exception.ValidationException;
import com.life.mapper.LifeFollowMapper;
import com.life.mapper.LifePostMapper;
import com.life.query.PostQueryDTO;
import com.life.result.PageResult;
import com.life.service.FavoriteService;
import com.life.service.LikeService;
import com.life.service.PostBloomService;
import com.life.service.PostFeedCacheService;
import com.life.service.PostHotService;
import com.life.service.PostSearchService;
import com.life.service.PostService;
import com.life.service.RiskService;
import com.life.service.SensitiveWordService;
import com.life.service.TaskCenterService;
import com.life.service.UserService;
import com.life.vo.AuthorVO;
import com.life.vo.PostVO;
import com.life.vo.RepostSourceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PostServiceImpl extends ServiceImpl<LifePostMapper, LifePost> implements PostService {

    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private LikeService likeService;
    @Autowired
    private FavoriteService favoriteService;
    @Autowired
    private LifeFollowMapper lifeFollowMapper;
    // ===== 2.11 热度推荐信息流 =====
    @Autowired
    private PostHotService postHotService;
    @Autowired
    private PostFeedCacheService postFeedCacheService;
    @Autowired
    private PostBloomService postBloomService;
    // ===== 2.9 审核 / 2.10 风控 / 2.4 任务 / 2.11 ES =====
    @Autowired
    private SensitiveWordService sensitiveWordService;
    @Autowired
    private RiskService riskService;
    @Autowired
    private TaskCenterService taskCenterService;
    @Autowired
    private PostSearchService postSearchService;
    @Autowired
    private com.life.mq.MqProducer mqProducer;

    /**
     * 发帖积分奖励(2026-11-12): 每篇可见帖经 MQ 异步发放; 每日限次防刷分(风控窗口之外的第二道闸)
     */
    @org.springframework.beans.factory.annotation.Value("${life.points.post-reward:10}")
    private int postRewardPoints;
    @org.springframework.beans.factory.annotation.Value("${life.points.post-reward-daily-limit:3}")
    private int postRewardDailyLimit;

    @Override
    public PostVO publishPost(PostPublishDTO postDTO) {
        // 1. 校验参数
        if (isBlank(postDTO.getContent())) {
            throw new ValidationException("帖子内容不能为空");
        }

        // 2. 获取当前登录用户(拦截器未覆盖或token失效时兜底)
        Long userId = BaseContext.getCurrentId();
        // 2.10 风控闸门: 被标记风险的用户暂停发帖
        riskService.checkAndCollect(RiskActionConstant.POST, userId);
        LifeUser user = userService.getById(userId);
        if (user == null) {
            throw new UserNotLoginException("用户不存在");
        }

        // 2.9 敏感词过滤(标题+正文合并判级): level1 拒绝 / level2 转人工审核(status=3) / level3 替换***
        SensitiveWordService.FilterResult titleFr = sensitiveWordService.filter(postDTO.getTitle());
        SensitiveWordService.FilterResult contentFr = sensitiveWordService.filter(postDTO.getContent());
        int level = mostSevere(titleFr, contentFr);
        if (level == 1) {
            throw new ValidationException("内容包含违规信息(命中: "
                    + String.join(",", titleFr.getWords()) + String.join(",", contentFr.getWords()) + "), 禁止发布");
        }

        // 3. 封装实体(BeanUtils会跳过类型不匹配的images, 单独序列化为JSON)
        LifePost post = buildPostEntity(postDTO, userId);
        if (level == 3) {
            // 替换级: 落库打码文本
            post.setTitle(titleFr.isHit() ? titleFr.getReplaced() : post.getTitle());
            post.setContent(contentFr.getReplaced());
        }
        post.setStatus(level == 2 ? PostStatusConstant.UNDER_REVIEW : PostStatusConstant.PUBLISHED);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);

        // 4. 落库
        post.setRepostCount(0);
        if (!save(post)) {
            throw new BaseException("帖子发布失败，请重试");
        }

        // 4.1 热度初始化(2.11): 基础分+新帖24h扶持即时入账, 布隆登记id, 首页缓存即时失效;
        //     审核通过的可见性翻转在 reviewPost 内补做ES索引
        postHotService.recalc(post.getId());
        postBloomService.add(post.getId());
        postFeedCacheService.evictHotFeed();
        if (post.getStatus() == PostStatusConstant.PUBLISHED) {
            postSearchService.indexPost(post);
            // 2026-11-12: 发帖积分经 MQ 异步发放(bizNo=POST:{id} 幂等, 每日限次)
            sendPostReward(post);
        }

        // 5. 发布成功后清理Redis草稿, 否则下次打开发布框仍会回填已发布内容
        redisTemplate.delete(draftKey(userId));

        // 6. 组装完整PostVO返回, 前端直接插入信息流(status=3 时为"审核中", 刷新后不进信息流)
        AuthorVO author = AuthorVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .followed(false)
                .build();
        return PostVO.builder()
                .id(post.getId())
                .author(author)
                .title(post.getTitle())
                .content(post.getContent())
                .images(postDTO.getImages())
                .category(post.getCategory())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .repostCount(0)
                .liked(false)
                .favorited(false)
                .status(post.getStatus())
                .createTime(post.getCreateTime())
                .build();
    }


    @Override
    public PostVO saveDraft(PostPublishDTO postDTO) {
        Long userId = BaseContext.getCurrentId();

        // 1. 草稿校验宽松: content可不填, 但title/content/images至少要有一个有值
        boolean empty = isBlank(postDTO.getContent()) && isBlank(postDTO.getTitle())
                && (postDTO.getImages() == null || postDTO.getImages().isEmpty());
        // 2. 为空则删除草稿记录
        if (empty) {
            redisTemplate.delete(draftKey(userId));
            return null;
        }

        // 3. 非空则只缓存DTO(前端关心的最小载荷), 丢弃无意义的id字段
        postDTO.setId(null);
        redisTemplate.opsForValue().set(draftKey(userId), JSON.toJSONString(postDTO),
                RedisConstant.POST_DRAFT_TTL_DAYS, TimeUnit.DAYS);

        // 4. 返回草稿VO(无id, Redis草稿仅用于回填编辑器)
        return toDraftVO(postDTO);
    }

    @Override
    public PostVO getDraft() {
        Long userId = BaseContext.getCurrentId();
        String jsonStr = redisTemplate.opsForValue().get(draftKey(userId));
        // 无草稿是正常态, 返回null由前端判空回填
        if (jsonStr == null) {
            return null;
        }
        PostPublishDTO draft;
        try {
            draft = JSON.parseObject(jsonStr, PostPublishDTO.class);
        } catch (Exception e) {
            // 脏数据/格式变更兜底: 删除坏草稿并返回null, 保证发布框不被卡死(P1修复)
            redisTemplate.delete(draftKey(userId));
            return null;
        }
        if (draft == null) {
            redisTemplate.delete(draftKey(userId));
            return null;
        }
        return toDraftVO(draft);
    }

    @Override
    public void clearDraft() {
        redisTemplate.delete(draftKey(BaseContext.getCurrentId()));
    }

    @Override
    public void deletePostById(Long postId) {
        Long userId = BaseContext.getCurrentId();
        // 先读源帖链接(转发帖删除需回冲计数; 归属校验仍在下方单条条件删除内完成, 无越权风险)
        LifePost post = getById(postId);
        // 归属校验放进DELETE条件, 单条语句原子完成, 消除查后删两步间的竞态(P2优化)
        boolean removed = remove(new LambdaQueryWrapper<LifePost>()
                .eq(LifePost::getId, postId)
                .eq(LifePost::getUserId, userId));
        if (!removed) {
            throw new BaseException("帖子不存在或无权限删除");
        }
        if (post != null && post.getRepostSourceId() != null) {
            baseMapper.decRepostCount(post.getRepostSourceId());
            // 源帖转发权重变化, 顺路刷新热度(布隆无删除语义: 误判存在由DB兜底, 无需回冲)
            postHotService.recalc(post.getRepostSourceId());
        }
        // 页码内容变化(该行从各页消失), 推荐流缓存即时失效; ES 索引同步移除(2.11)
        postFeedCacheService.evictHotFeed();
        postSearchService.removePost(postId);
    }

    /**
     * 转发帖子(1.5 一期): 生成转发帖 + 源帖计数原子+1, 同事务保证"计数与转发帖必同时存在";
     * 源帖并发被删时 inc 影响0行 -> 整体回滚, 不产生孤儿转发帖
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVO repost(Long postId, String remark) {
        Long userId = BaseContext.getCurrentId();
        // 2.11 穿透防线: 布隆判定"必不存在"直接拒绝, 不打DB(过滤器未就绪/异常时 fail-open 照常走)
        if (!postBloomService.mightContain(postId)) {
            throw new ValidationException("帖子不存在或不可转发");
        }
        LifePost source = getById(postId);
        if (source == null || source.getStatus() == null
                || (source.getStatus() != PostStatusConstant.PUBLISHED
                && source.getStatus() != PostStatusConstant.TOP)) {
            throw new ValidationException("帖子不存在或不可转发");
        }
        // 2.9 附言敏感词过滤(转发帖无审核态, level1/2 同口径拒绝, level3 打码)
        String cleanRemark = remark == null ? "" : remark.trim();
        if (!cleanRemark.isEmpty()) {
            SensitiveWordService.FilterResult fr = sensitiveWordService.filter(cleanRemark);
            if (fr.isHit() && fr.getMaxLevel() <= 2) {
                throw new ValidationException("附言包含违规内容, 请修改后再试");
            }
            if (fr.isHit()) {
                cleanRemark = fr.getReplaced();
            }
        }

        LifePost repost = new LifePost();
        repost.setUserId(userId);
        repost.setTitle("");
        // 附言可空(纯转发), 与原创帖同口径入库; 转发不带图/分类
        repost.setContent(cleanRemark);
        repost.setStatus(PostStatusConstant.PUBLISHED);
        repost.setViewCount(0);
        repost.setLikeCount(0);
        repost.setCommentCount(0);
        repost.setRepostCount(0);
        repost.setRepostSourceId(postId);
        save(repost);

        if (baseMapper.incRepostCount(postId) == 0) {
            throw new BaseException("源帖已被删除, 转发失败");
        }

        // 热度联动(2.11): 源帖转发权重+1、转发帖基础分+新帖扶持各自重算;
        // 同事务内可见最新计数, 提交前失效缓存(极小概率被并发读回填旧页, 短TTL内自愈)
        postBloomService.add(repost.getId());
        postHotService.recalc(postId);
        postHotService.recalc(repost.getId());
        postFeedCacheService.evictHotFeed();
        postSearchService.indexPost(repost);
        // 2.4 任务事件: 分享帖子任务落完成记录
        taskCenterService.triggerEvent(userId, TaskEventTypeConstant.SHARE);

        // 组装返回VO: 本人作者信息 + 源帖摘要, 前端直接插入信息流
        LifeUser me = userService.getById(userId);
        if (me == null) {
            throw new UserNotLoginException("用户不存在");
        }
        AuthorVO author = AuthorVO.builder()
                .userId(me.getId())
                .username(me.getUsername())
                .nickname(me.getNickname())
                .avatar(me.getAvatar())
                .followed(false)
                .build();
        Map<Long, LifeUser> sourceUserMap = new HashMap<>();
        LifeUser sourceAuthor = userService.getById(source.getUserId());
        if (sourceAuthor != null) {
            sourceUserMap.put(sourceAuthor.getId(), sourceAuthor);
        }
        return PostVO.builder()
                .id(repost.getId())
                .author(author)
                .title(repost.getTitle())
                .content(repost.getContent())
                .images(List.of())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .repostCount(0)
                .repostSourceId(postId)
                .repostSource(buildRepostSource(postId, source, sourceUserMap))
                .liked(false)
                .favorited(false)
                .status(repost.getStatus())
                .createTime(repost.getCreateTime())
                .build();
    }

    @Override
    public PageResult pageQuery(PostQueryDTO query) {
        Long me = BaseContext.getCurrentId();
        // 关注流子查询依赖登录态, 强制覆盖防前端伪造
        query.setCurrentUserId(me);

        long total;
        List<LifePost> posts;
        PostSearchService.SearchResult esResult = tryEsSearch(query);
        if (esResult != null) {
            // 2.11 ES 全文检索路径: 相关度排序的id → 回表取行(保持ES顺序)
            total = esResult.getTotal();
            posts = hydrateByIds(esResult.getIds());
        } else if (isPureHotFeed(query)) {
            // 2.11 多级缓存: 纯推荐流(无用户/关键词/分类条件)按实体页缓存,
            // 用户态字段(liked/favorited/followed)仍在本方法后续实时组装
            PostFeedCacheService.FeedPageSnapshot snapshot = postFeedCacheService.loadHotFeedPage(
                    query.getPageNum(), query.getPageSize(),
                    () -> baseMapper.queryPostPage(
                            new Page<>(query.getPageNum(), query.getPageSize()), query));
            total = snapshot.getTotal();
            posts = snapshot.getRecords();
        } else {
            Page<LifePost> page = new Page<>(query.getPageNum(), query.getPageSize());
            Page<LifePost> resultPage = baseMapper.queryPostPage(page, query);
            total = resultPage.getTotal();
            posts = resultPage.getRecords();
        }
        if (posts == null || posts.isEmpty()) {
            return new PageResult(total, List.of());
        }

        // 批量组装: 作者信息 / 我是否已关注 / 我是否已点赞(Redis实时)
        List<Long> authorIds = posts.stream().map(LifePost::getUserId).distinct().toList();
        Map<Long, LifeUser> userMap = userService.listByIds(authorIds).stream()
                .collect(Collectors.toMap(LifeUser::getId, Function.identity()));

        Set<Long> followedIds = lifeFollowMapper.selectList(new LambdaQueryWrapper<LifeFollow>()
                        .select(LifeFollow::getFollowUserId)
                        .eq(LifeFollow::getUserId, me)
                        .in(LifeFollow::getFollowUserId, authorIds))
                .stream().map(LifeFollow::getFollowUserId).collect(Collectors.toSet());

        List<Long> postIds = posts.stream().map(LifePost::getId).toList();
        Set<Long> likedIds = new HashSet<>(likeService.isBizLiked(BizTypeConstant.POST, postIds));
        // 收藏角标批量状态(DB唯一键前缀 user_id 索引, 直查即可; 口径参照 liked)
        Set<Long> favoritedIds = new HashSet<>(favoriteService.listFavoritedBizIds(
                com.life.constant.FavoriteTypeConstant.POST, postIds));

        // 转发源帖批量: 收集源帖ID(去重非空)一次IN查, 作者并入userMap复用同批查询
        List<Long> sourceIds = posts.stream().map(LifePost::getRepostSourceId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, LifePost> sourceMap = new HashMap<>();
        if (!sourceIds.isEmpty()) {
            List<LifePost> sources = listByIds(sourceIds);
            sources.forEach(s -> sourceMap.put(s.getId(), s));
            // 源帖作者若不在当前页作者集内, 追加一次批量查询
            List<Long> extraAuthorIds = sources.stream().map(LifePost::getUserId)
                    .filter(uid -> !userMap.containsKey(uid)).distinct().toList();
            if (!extraAuthorIds.isEmpty()) {
                userService.listByIds(extraAuthorIds).forEach(u -> userMap.put(u.getId(), u));
            }
        }

        List<PostVO> vos = posts.stream().map(p -> {
            LifeUser u = userMap.get(p.getUserId());
            AuthorVO author = AuthorVO.builder()
                    .userId(p.getUserId())
                    .username(u == null ? "" : u.getUsername())
                    .nickname(u == null ? "用户已注销" : u.getNickname())
                    .avatar(u == null ? null : u.getAvatar())
                    .followed(followedIds.contains(p.getUserId()))
                    .build();
            return PostVO.builder()
                    .id(p.getId())
                    .author(author)
                    .title(p.getTitle())
                    .content(p.getContent())
                    .images(parseImages(p.getImages()))
                    .category(p.getCategory())
                    .viewCount(p.getViewCount())
                    .likeCount(p.getLikeCount())
                    .commentCount(p.getCommentCount())
                    .repostCount(p.getRepostCount() == null ? 0 : p.getRepostCount())
                    .repostSourceId(p.getRepostSourceId())
                    .repostSource(p.getRepostSourceId() == null ? null
                            : buildRepostSource(p.getRepostSourceId(), sourceMap.get(p.getRepostSourceId()), userMap))
                    .liked(likedIds.contains(p.getId()))
                    .favorited(favoritedIds.contains(p.getId()))
                    .status(p.getStatus())
                    .createTime(p.getCreateTime())
                    .build();
        }).toList();

        return new PageResult(total, vos);
    }

    /**
     * 2.9 管理端待审列表: status=3(审核中) 时间倒序, 直接回实体行(管理端无用户态组装需求)
     */
    @Override
    public PageResult queryReviewPage(PostQueryDTO query) {
        Page<LifePost> page = this.page(new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<LifePost>()
                        .eq(LifePost::getStatus, PostStatusConstant.UNDER_REVIEW)
                        .orderByDesc(LifePost::getCreateTime));
        return new PageResult(page.getTotal(), page.getRecords());
    }

    /**
     * 2.9 管理端审核动作: CAS 3→1(通过, 进信息流+ES索引) / 3→4(驳回, 不可见+移出索引);
     * 重复审核/已被处理返回0行 → 报"状态已变化"
     */
    @Override
    public void reviewPost(Long postId, boolean pass) {
        int target = pass ? PostStatusConstant.PUBLISHED : PostStatusConstant.DELETED;
        int updated = baseMapper.update(null, new LambdaUpdateWrapper<LifePost>()
                .eq(LifePost::getId, postId)
                .eq(LifePost::getStatus, PostStatusConstant.UNDER_REVIEW)
                .set(LifePost::getStatus, target)
                .set(LifePost::getUpdateTime, java.time.LocalDateTime.now()));
        if (updated == 0) {
            throw new ValidationException("帖子不存在或已审核, 请刷新列表");
        }
        if (pass) {
            postSearchService.indexPost(getById(postId));
            // 审核过审补发发帖积分(与发布时同 bizNo=POST:{id}, 双触发只入账一次; 发布时若超每日限次此处亦受限)
            LifePost approved = getById(postId);
            if (approved != null) {
                sendPostReward(approved);
            }
        } else {
            postSearchService.removePost(postId);
        }
        // 审核结果改变信息流内容, 缓存即时失效 + 热度重算(过审帖获得基础分入流)
        postFeedCacheService.evictHotFeed();
        postHotService.recalc(postId);
    }

    // ============ 私有方法 ============

    /**
     * 2.11 ES 检索准入: 仅推荐流纯关键词场景(无 userId/category 叠加过滤, 分类+关键词组合走SQL);
     * ES 不可用/搜索失败返回 null → 调用方降级 SQL LIKE, 搜索功能不中断
     */
    private PostSearchService.SearchResult tryEsSearch(PostQueryDTO query) {
        boolean hotType = query.getType() == null || query.getType() == 1;
        if (!hotType || query.getUserId() != null || !isBlank(query.getCategory())
                || isBlank(query.getKeyword())) {
            return null;
        }
        if (!postSearchService.available()) {
            return null;
        }
        return postSearchService.search(query.getKeyword().trim(), query.getPageNum(), query.getPageSize());
    }

    /**
     * ES 命中 id 回表: 保持相关度顺序, 过滤已删/非可见行(索引双写失败的自愈兜底)
     */
    private List<LifePost> hydrateByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<Long, LifePost> map = listByIds(ids).stream()
                .collect(Collectors.toMap(LifePost::getId, Function.identity()));
        return ids.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .filter(p -> p.getStatus() != null && p.getStatus() == PostStatusConstant.PUBLISHED)
                .toList();
    }

    /**
     * 纯推荐流口径(2.11 缓存准入): type=1或缺省、无userId/keyword/category过滤——
     * 结果与当前用户无关才允许跨用户共享缓存; 其余口径仍直查DB
     */
    private boolean isPureHotFeed(PostQueryDTO query) {
        boolean hotType = query.getType() == null || query.getType() == 1;
        return hotType
                && query.getUserId() == null
                && isBlank(query.getKeyword())
                && isBlank(query.getCategory());
    }

    /**
     * 发帖积分投递(2026-11-12): 仅 status=已发布 的帖参与; 每日限次(含本篇)超出即不投;
     * bizNo=POST:{postId} —— 发布/过审双触发与 MQ 重投均由 uk_biz_no 收敛为一次入账;
     * MQ 不可用时静默跳过(发帖主流程不受影响, 奖励属激励非账务)
     */
    private void sendPostReward(LifePost post) {
        if (post == null || post.getStatus() == null
                || post.getStatus() != PostStatusConstant.PUBLISHED) {
            return;
        }
        try {
            long todayCount = count(new LambdaQueryWrapper<LifePost>()
                    .eq(LifePost::getUserId, post.getUserId())
                    .ge(LifePost::getCreateTime, java.time.LocalDate.now().atStartOfDay()));
            if (todayCount > postRewardDailyLimit) {
                log.debug("发帖积分超每日限次 userId={} today={} limit={}",
                        post.getUserId(), todayCount, postRewardDailyLimit);
                return;
            }
            mqProducer.sendQuiet(com.life.constant.MqConstant.POINTS_EXCHANGE,
                    com.life.constant.MqConstant.POINTS_POST_ROUTING_KEY,
                    com.life.dto.MqPointsMessage.builder()
                            .userId(post.getUserId())
                            .points(postRewardPoints)
                            .bizNo("POST:" + post.getId())
                            .remark("发帖奖励")
                            .build());
        } catch (Exception e) {
            log.warn("发帖积分投递失败(不阻断发帖) postId={}", post.getId(), e);
        }
    }

    /**
     * 取标题/正文两次过滤的最严命中级别(数字越小越严), 0=全部未命中
     */
    private int mostSevere(SensitiveWordService.FilterResult a, SensitiveWordService.FilterResult b) {
        int la = a.isHit() ? a.getMaxLevel() : 0;
        int lb = b.isHit() ? b.getMaxLevel() : 0;
        if (la == 0) {
            return lb;
        }
        if (lb == 0) {
            return la;
        }
        return Math.min(la, lb);
    }

    /**
     * DTO转实体公共部分: userId + 各字段拷贝 + images序列化 + status=1(发布默认)
     */
    private LifePost buildPostEntity(PostPublishDTO dto, Long userId) {
        LifePost post = new LifePost();
        BeanUtils.copyProperties(dto, post);
        post.setUserId(userId);
        // title 列 NOT NULL: 前端纯文本流不传标题时置空串, 避免约束异常
        if (post.getTitle() == null) {
            post.setTitle("");
        }
        post.setImages(dto.getImages() == null || dto.getImages().isEmpty()
                ? null : JSON.toJSONString(dto.getImages()));
        return post;
    }

    /**
     * 草稿Redis key: post:draft:uid{userId}, 与签到sign:uid命名对齐(P2抽公共)
     */
    private String draftKey(Long userId) {
        return RedisConstant.POST_DRAFT.replace("{userId}", userId.toString());
    }

    /**
     * 草稿DTO转VO(无id/计数/时间, 仅供编辑器回填; Redis草稿不记录保存时间,
     * 若后续需要"上次编辑于xx"展示, 再引入带savedAt的缓存包装)
     */
    private PostVO toDraftVO(PostPublishDTO dto) {
        return PostVO.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .images(dto.getImages())
                .category(dto.getCategory())
                .status(PostStatusConstant.DRAFT)
                .build();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * 图片JSON数组字符串转列表, 解析异常时降级为空列表不影响展示
     */
    private List<String> parseImages(String images) {
        if (isBlank(images)) {
            return List.of();
        }
        try {
            return JSON.parseArray(images, String.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 组装转发源帖摘要: source=null 表示源帖已被物理/逻辑删除 -> available=false 占位;
     * 正文超140字符截断, 图片取首图, 作者昵称空以username兜底
     */
    private RepostSourceVO buildRepostSource(Long sourceId, LifePost source, Map<Long, LifeUser> userMap) {
        if (source == null) {
            return RepostSourceVO.builder()
                    .id(sourceId)
                    .authorName("用户已注销")
                    .content("")
                    .available(false)
                    .build();
        }
        LifeUser u = userMap.get(source.getUserId());
        String authorName = u == null ? "用户已注销"
                : (!isBlank(u.getNickname()) ? u.getNickname() : u.getUsername());
        String content = source.getContent();
        if (content != null && content.length() > 140) {
            content = content.substring(0, 140);
        }
        List<String> imgs = parseImages(source.getImages());
        return RepostSourceVO.builder()
                .id(sourceId)
                .authorName(authorName)
                .content(content)
                .image(imgs.isEmpty() ? null : imgs.get(0))
                .createTime(source.getCreateTime())
                .available(true)
                .build();
    }
}

package com.life.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.BizTypeConstant;
import com.life.constant.PostStatusConstant;
import com.life.constant.RedisKeyConstant;
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
import com.life.result.Result;
import com.life.service.FavoriteService;
import com.life.service.LikeService;
import com.life.service.PostService;
import com.life.service.UserService;
import com.life.vo.AuthorVO;
import com.life.vo.PostVO;
import com.life.vo.RepostSourceVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    // @Override
    // public PostVO publishPost(PostPublishDTO postDTO) {
    //     // 1. 校验参数
    //     if (isBlank(postDTO.getContent())) {
    //         throw new ValidationException("帖子内容不能为空");
    //     }
    //
    //     // 2. 获取当前登录用户(拦截器未覆盖或token失效时兜底)
    //     Long userId = requireUserId();
    //     LifeUser user = userService.getById(userId);
    //     if (user == null) {
    //         throw new UserNotLoginException("用户不存在");
    //     }
    //
    //     // 3. 封装实体(BeanUtils会跳过类型不匹配的images, 单独序列化为JSON)
    //     LifePost post = buildPostEntity(postDTO, userId);
    //     post.setStatus(1);
    //
    //     // 4. 落库: 带id走"草稿转正"(单行UPDATE), 否则新增(单行INSERT)
    //     //    两种路径都只有一条写语句, 不存在"发帖成功但清草稿失败"的半成功态
    //     if (postDTO.getId() != null) {
    //         LifePost draft = getOne(new LambdaQueryWrapper<LifePost>()
    //                 .eq(LifePost::getId, postDTO.getId())
    //                 .eq(LifePost::getUserId, userId)
    //                 .eq(LifePost::getStatus, 0));
    //         if (draft == null) {
    //             throw new BaseException("草稿不存在、已发布或无权限操作");
    //         }
    //         post.setId(draft.getId());
    //         // 发布时间以点击"发布"为准, 避免信息流里出现草稿创建时间与发布时间错开
    //         post.setCreateTime(LocalDateTime.now());
    //         if (!updateById(post)) {
    //             throw new BaseException("帖子发布失败，请重试");
    //         }
    //     } else {
    //         post.setViewCount(0);
    //         post.setLikeCount(0);
    //         post.setCommentCount(0);
    //         if (!save(post)) {
    //             throw new BaseException("帖子发布失败，请重试");
    //         }
    //     }
    //
    //     // 5. 发布成功: 清理该用户遗留草稿(转正行已变status=1不受影响; 直接发布场景清掉旧草稿)
    //     clearDraftByUser(userId);
    //
    //     // 6. 组装完整PostVO返回, 前端直接插入信息流头部
    //     AuthorVO author = AuthorVO.builder()
    //             .userId(user.getId())
    //             .username(user.getUsername())
    //             .nickname(user.getNickname())
    //             .avatar(user.getAvatar())
    //             .followed(false)
    //             .build();
    //     return PostVO.builder()
    //             .id(post.getId())
    //             .author(author)
    //             .title(post.getTitle())
    //             .content(post.getContent())
    //             .images(postDTO.getImages())
    //             .category(post.getCategory())
    //             .viewCount(post.getViewCount())
    //             .likeCount(post.getLikeCount())
    //             .commentCount(post.getCommentCount())
    //             .repostCount(0)
    //             .liked(false)
    //             .status(post.getStatus())
    //             .createTime(post.getCreateTime())
    //             .build();
    // }
    // @Override
    // public PostVO saveDraft(PostPublishDTO postDTO) {
    //     Long userId = requireUserId();
    //
    //     // 1. 草稿校验宽松: content可不填, 但title/content/images至少要有一个有值
    //     boolean empty = isBlank(postDTO.getContent()) && isBlank(postDTO.getTitle())
    //             && (postDTO.getImages() == null || postDTO.getImages().isEmpty());
    //     if (empty) {
    //         // 空内容PUT视为"放弃草稿": 静默清理, 幂等
    //         clearDraftByUser(userId);
    //         return null;
    //     }
    //
    //     // 2. 定位可写入的草稿行: 优先DTO指定id, 否则取本人最新草稿(一人一份语义)
    //     LifePost target;
    //     if (postDTO.getId() != null) {
    //         target = getOne(new LambdaQueryWrapper<LifePost>()
    //                 .eq(LifePost::getId, postDTO.getId())
    //                 .eq(LifePost::getUserId, userId)
    //                 .eq(LifePost::getStatus, 0));
    //         if (target == null) {
    //             throw new BaseException("草稿不存在、已发布或无权限操作");
    //         }
    //     } else {
    //         target = getOne(new LambdaQueryWrapper<LifePost>()
    //                 .eq(LifePost::getUserId, userId)
    //                 .eq(LifePost::getStatus, 0)
    //                 .orderByDesc(LifePost::getId)
    //                 .last("LIMIT 1"), false);
    //     }
    //
    //     LifePost post = buildPostEntity(postDTO, userId);
    //
    //     // 3. 有草稿则更新, 无则新建
    //     if (target != null) {
    //         post.setId(target.getId());
    //         if (!updateById(post)) {
    //             throw new BaseException("草稿保存失败，请重试");
    //         }
    //     } else {
    //         post.setStatus(0);
    //         post.setViewCount(0);
    //         post.setLikeCount(0);
    //         post.setCommentCount(0);
    //         if (!save(post)) {
    //             throw new BaseException("草稿保存失败，请重试");
    //         }
    //     }
    //     // 4. 返回草稿VO(含id), 前端记住id用于后续"带id发布转正"
    //     return toDraftVO(post, postDTO.getImages());
    // }
    // @Override
    // public PostVO getDraft() {
    //     Long userId = requireUserId();
    //     LifePost draft = getOne(new LambdaQueryWrapper<LifePost>()
    //             .eq(LifePost::getUserId, userId)
    //             .eq(LifePost::getStatus, 0)
    //             .orderByDesc(LifePost::getId)
    //             .last("LIMIT 1"), false);
    //     // 无草稿是正常态, 返回null由前端判空回填
    //     if (draft == null) {
    //         return null;
    //     }
    //     return toDraftVO(draft, parseImages(draft.getImages()));
    // }
    //
    // @Override
    // public void clearDraft() {
    //     clearDraftByUser(requireUserId());
    // }
    //
    // @Override
    // public void deletePostById(Long postId) {
    //     Long userId = requireUserId();
    //     LifePost post = getOne(new LambdaQueryWrapper<LifePost>()
    //             .eq(LifePost::getId, postId)
    //             .eq(LifePost::getUserId, userId));
    //     if (post == null) {
    //         throw new BaseException("帖子不存在或无权限删除");
    //     }
    //     removeById(postId);
    // }

    @Override
    public PostVO publishPost(PostPublishDTO postDTO) {
        // 1. 校验参数
        if (isBlank(postDTO.getContent())) {
            throw new ValidationException("帖子内容不能为空");
        }

        // 2. 获取当前登录用户(拦截器未覆盖或token失效时兜底)
        Long userId = BaseContext.getCurrentId();
        LifeUser user = userService.getById(userId);
        if (user == null) {
            throw new UserNotLoginException("用户不存在");
        }

        // 3. 封装实体(BeanUtils会跳过类型不匹配的images, 单独序列化为JSON)
        LifePost post = buildPostEntity(postDTO, userId);
        post.setStatus(PostStatusConstant.PUBLISHED);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);

        // 4. 落库
        post.setRepostCount(0);
        if (!save(post)) {
            throw new BaseException("帖子发布失败，请重试");
        }

        // 5. 发布成功后清理Redis草稿, 否则下次打开发布框仍会回填已发布内容
        redisTemplate.delete(draftKey(userId));

        // 6. 组装完整PostVO返回, 前端直接插入信息流头部
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
                RedisKeyConstant.POST_DRAFT_TTL_DAYS, TimeUnit.DAYS);

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
        }
    }

    /**
     * 转发帖子(1.5 一期): 生成转发帖 + 源帖计数原子+1, 同事务保证"计数与转发帖必同时存在";
     * 源帖并发被删时 inc 影响0行 -> 整体回滚, 不产生孤儿转发帖
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVO repost(Long postId, String remark) {
        Long userId = BaseContext.getCurrentId();
        LifePost source = getById(postId);
        if (source == null || source.getStatus() == null
                || (source.getStatus() != PostStatusConstant.PUBLISHED
                && source.getStatus() != PostStatusConstant.TOP)) {
            throw new ValidationException("帖子不存在或不可转发");
        }

        LifePost repost = new LifePost();
        repost.setUserId(userId);
        repost.setTitle("");
        // 附言可空(纯转发), 与原创帖同口径入库; 转发不带图/分类
        repost.setContent(remark == null ? "" : remark.trim());
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

        Page<LifePost> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<LifePost> resultPage = baseMapper.queryPostPage(page, query);
        List<LifePost> posts = resultPage.getRecords();
        if (posts == null || posts.isEmpty()) {
            return new PageResult(resultPage.getTotal(), List.of());
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
                .filter(java.util.Objects::nonNull).distinct().toList();
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

        return new PageResult(resultPage.getTotal(), vos);
    }

    // ============ 私有方法 ============

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
        return RedisKeyConstant.POST_DRAFT.replace("{userId}", userId.toString());
    }

    // private void clearDraftByUser(Long userId) {
    //     remove(new LambdaQueryWrapper<LifePost>()
    //             .eq(LifePost::getUserId, userId)
    //             .eq(LifePost::getStatus, 0));
    // }

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

    // 【草稿改存DTO后未使用, 按要求保留备查】实体版草稿VO组装
    // private PostVO toDraftVO(LifePost post, List<String> images) {
    //     return PostVO.builder()
    //             .id(post.getId())
    //             .title(post.getTitle())
    //             .content(post.getContent())
    //             .images(images)
    //             .category(post.getCategory())
    //             .status(post.getStatus())
    //             .createTime(post.getCreateTime())
    //             .build();
    // }

    // 【草稿改存DTO后未使用, 按要求保留备查】图片JSON数组字符串转列表, 解析异常时降级为空列表不影响展示
    // private List<String> parseImages(String images) {
    //     if (isBlank(images)) {
    //         return List.of();
    //     }
    //     try {
    //         return JSON.parseArray(images, String.class);
    //     } catch (Exception e) {
    //         return List.of();
    //     }
    // }

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

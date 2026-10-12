package com.life.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.PostStatusConstant;
import com.life.constant.RedisKeyConstant;
import com.life.context.BaseContext;
import com.life.dto.PostPublishDTO;
import com.life.entity.LifePost;
import com.life.entity.LifeUser;
import com.life.exception.BaseException;
import com.life.exception.UserNotLoginException;
import com.life.exception.ValidationException;
import com.life.mapper.LifePostMapper;
import com.life.result.Result;
import com.life.service.PostService;
import com.life.service.UserService;
import com.life.vo.AuthorVO;
import com.life.vo.PostVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PostServiceImpl extends ServiceImpl<LifePostMapper, LifePost> implements PostService {

    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate redisTemplate;

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
        // 归属校验放进DELETE条件, 单条语句原子完成, 消除查后删两步间的竞态(P2优化)
        boolean removed = remove(new LambdaQueryWrapper<LifePost>()
                .eq(LifePost::getId, postId)
                .eq(LifePost::getUserId, userId));
        if (!removed) {
            throw new BaseException("帖子不存在或无权限删除");
        }
    }

    // ============ 私有方法 ============

    /**
     * DTO转实体公共部分: userId + 各字段拷贝 + images序列化 + status=1(发布默认)
     */
    private LifePost buildPostEntity(PostPublishDTO dto, Long userId) {
        LifePost post = new LifePost();
        BeanUtils.copyProperties(dto, post);
        post.setUserId(userId);
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
}

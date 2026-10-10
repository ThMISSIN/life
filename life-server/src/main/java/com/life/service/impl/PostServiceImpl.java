package com.life.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.context.BaseContext;
import com.life.dto.PostPublishDTO;
import com.life.entity.LifePost;
import com.life.entity.LifeUser;
import com.life.exception.BaseException;
import com.life.exception.UserNotLoginException;
import com.life.exception.ValidationException;
import com.life.mapper.LifePostMapper;
import com.life.service.PostService;
import com.life.service.UserService;
import com.life.vo.AuthorVO;
import com.life.vo.PostVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostServiceImpl extends ServiceImpl<LifePostMapper, LifePost> implements PostService {

    @Autowired
    private UserService userService;

    @Override
    public PostVO publishPost(PostPublishDTO postDTO) {
        // 1. 校验参数
        if (isBlank(postDTO.getContent())) {
            throw new ValidationException("帖子内容不能为空");
        }

        // 2. 获取当前登录用户(拦截器未覆盖或token失效时兜底)
        Long userId = requireUserId();
        LifeUser user = userService.getById(userId);
        if (user == null) {
            throw new UserNotLoginException("用户不存在");
        }

        // 3. 封装实体(BeanUtils会跳过类型不匹配的images, 单独序列化为JSON)
        LifePost post = buildPostEntity(postDTO, userId);
        post.setStatus(1);

        // 4. 落库: 带id走"草稿转正"(单行UPDATE), 否则新增(单行INSERT)
        //    两种路径都只有一条写语句, 不存在"发帖成功但清草稿失败"的半成功态
        if (postDTO.getId() != null) {
            LifePost draft = getOne(new LambdaQueryWrapper<LifePost>()
                    .eq(LifePost::getId, postDTO.getId())
                    .eq(LifePost::getUserId, userId)
                    .eq(LifePost::getStatus, 0));
            if (draft == null) {
                throw new BaseException("草稿不存在、已发布或无权限操作");
            }
            post.setId(draft.getId());
            // 发布时间以点击"发布"为准, 避免信息流里出现草稿创建时间与发布时间错开
            post.setCreateTime(LocalDateTime.now());
            if (!updateById(post)) {
                throw new BaseException("帖子发布失败，请重试");
            }
        } else {
            post.setViewCount(0);
            post.setLikeCount(0);
            post.setCommentCount(0);
            if (!save(post)) {
                throw new BaseException("帖子发布失败，请重试");
            }
        }

        // 5. 发布成功: 清理该用户遗留草稿(转正行已变status=1不受影响; 直接发布场景清掉旧草稿)
        clearDraftByUser(userId);

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
        Long userId = requireUserId();

        // 1. 草稿校验宽松: content可不填, 但title/content/images至少要有一个有值
        boolean empty = isBlank(postDTO.getContent()) && isBlank(postDTO.getTitle())
                && (postDTO.getImages() == null || postDTO.getImages().isEmpty());
        if (empty) {
            // 空内容PUT视为"放弃草稿": 静默清理, 幂等
            clearDraftByUser(userId);
            return null;
        }

        // 2. 定位可写入的草稿行: 优先DTO指定id, 否则取本人最新草稿(一人一份语义)
        LifePost target;
        if (postDTO.getId() != null) {
            target = getOne(new LambdaQueryWrapper<LifePost>()
                    .eq(LifePost::getId, postDTO.getId())
                    .eq(LifePost::getUserId, userId)
                    .eq(LifePost::getStatus, 0));
            if (target == null) {
                throw new BaseException("草稿不存在、已发布或无权限操作");
            }
        } else {
            target = getOne(new LambdaQueryWrapper<LifePost>()
                    .eq(LifePost::getUserId, userId)
                    .eq(LifePost::getStatus, 0)
                    .orderByDesc(LifePost::getId)
                    .last("LIMIT 1"), false);
        }

        LifePost post = buildPostEntity(postDTO, userId);

        // 3. 有草稿则更新, 无则新建
        if (target != null) {
            post.setId(target.getId());
            if (!updateById(post)) {
                throw new BaseException("草稿保存失败，请重试");
            }
        } else {
            post.setStatus(0);
            post.setViewCount(0);
            post.setLikeCount(0);
            post.setCommentCount(0);
            if (!save(post)) {
                throw new BaseException("草稿保存失败，请重试");
            }
        }
        // 4. 返回草稿VO(含id), 前端记住id用于后续"带id发布转正"
        return toDraftVO(post, postDTO.getImages());
    }

    @Override
    public PostVO getDraft() {
        Long userId = requireUserId();
        LifePost draft = getOne(new LambdaQueryWrapper<LifePost>()
                .eq(LifePost::getUserId, userId)
                .eq(LifePost::getStatus, 0)
                .orderByDesc(LifePost::getId)
                .last("LIMIT 1"), false);
        // 无草稿是正常态, 返回null由前端判空回填
        if (draft == null) {
            return null;
        }
        return toDraftVO(draft, parseImages(draft.getImages()));
    }

    @Override
    public void clearDraft() {
        clearDraftByUser(requireUserId());
    }

    @Override
    public void deletePostById(Long postId) {
        Long userId = requireUserId();
        LifePost post = getOne(new LambdaQueryWrapper<LifePost>()
                .eq(LifePost::getId, postId)
                .eq(LifePost::getUserId, userId));
        if (post == null) {
            throw new BaseException("帖子不存在或无权限删除");
        }
        removeById(postId);
    }

    // ============ 私有方法 ============

    /**
     * 取当前登录用户ID, 拦截器兜底
     */
    private Long requireUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new UserNotLoginException("用户未登录或登录已过期");
        }
        return userId;
    }

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
     * 清理指定用户全部草稿(status=0), 逻辑删除, 幂等
     */
    private void clearDraftByUser(Long userId) {
        remove(new LambdaQueryWrapper<LifePost>()
                .eq(LifePost::getUserId, userId)
                .eq(LifePost::getStatus, 0));
    }

    private PostVO toDraftVO(LifePost post, List<String> images) {
        return PostVO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .images(images)
                .category(post.getCategory())
                .status(post.getStatus())
                .createTime(post.getCreateTime())
                .build();
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

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

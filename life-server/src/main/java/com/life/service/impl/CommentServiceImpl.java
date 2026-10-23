package com.life.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.context.BaseContext;
import com.life.dto.CommentPublishDTO;
import com.life.dto.PageDTO;
import com.life.entity.LifeComment;
import com.life.entity.LifePost;
import com.life.entity.LifeUser;
import com.life.exception.ValidationException;
import com.life.mapper.LifeCommentMapper;
import com.life.mapper.LifePostMapper;
import com.life.query.CommentQueryDTO;
import com.life.service.CommentService;

import com.life.service.UserService;
import com.life.utils.CollUtils;
import com.life.vo.AuthorVO;
import com.life.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<LifeCommentMapper, LifeComment> implements CommentService {

    @Autowired
    private UserService userService;
    @Autowired
    private LifePostMapper postMapper;

    @Override
    public void addComment(CommentPublishDTO replyDTO) {
        if (replyDTO.getPostId() == null || replyDTO.getContent() == null) {
            throw new IllegalArgumentException("帖子ID和评论内容不能为空");
        }
        // 将dto转为entity
        LifeComment lifeComment = BeanUtil.copyProperties(replyDTO, LifeComment.class);
        lifeComment.setUserId(BaseContext.getCurrentId());
        // 判断是否为顶级评论
        if (replyDTO.getParentId() != null) {
            lifeComment.setParentId(replyDTO.getParentId());
        }
        else {
            lifeComment.setParentId(0L);
        }
        // 保存到数据库
        save(lifeComment);
    }

    @Override
    public PageDTO<CommentVO> queryCommentPage(CommentQueryDTO query) {
        // parentId 不传或为0表示查询顶级评论，否则查询该父评论下的子评论
        Long parentId = query.getParentId();
        boolean isParent = parentId != null && parentId != 0L;

        LambdaQueryWrapper<LifeComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LifeComment::getPostId, query.getPostId())
                .eq(LifeComment::getParentId, isParent ? parentId : 0L)
                .orderByDesc(LifeComment::getLikeCount, LifeComment::getCreateTime);
        Page<LifeComment> page = this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        // 将entity转为vo
        List<LifeComment> records = page.getRecords();
        List<CommentVO> voList = new ArrayList<>();

        // 分页查询子评论时，直接平铺返回
        if (isParent) {
            for (LifeComment child : records) {
                CommentVO childVO = BeanUtil.copyProperties(child, CommentVO.class);
                childVO.setAuthor(buildAuthor(child.getUserId()));
                voList.add(childVO);
            }
            return new PageDTO<>(page.getTotal(), page.getPages(), voList);
        }

        if (CollUtils.isEmpty(records)) {
            return new PageDTO<>(page.getTotal(), page.getPages(), voList);
        }

        // 查询当前页所有父评论下的子评论
        List<Long> parentIds = records.stream().map(LifeComment::getId).toList();
        List<LifeComment> childComments = this.list(new LambdaQueryWrapper<LifeComment>()
                .in(LifeComment::getParentId, parentIds)
                .orderByDesc(LifeComment::getCreateTime));
        // 按父评论分组 parentId + ChildrenCommnet
        Map<Long, List<LifeComment>> childMap = childComments.stream()
                .collect(Collectors.groupingBy(LifeComment::getParentId));

        for (LifeComment parent : records) {
            // 封装为vo
            CommentVO parentVO = BeanUtil.copyProperties(parent, CommentVO.class);
            parentVO.setAuthor(buildAuthor(parent.getUserId()));

            // 封装子评论
            parentVO.setChildren(childMap.getOrDefault(parent.getId(), CollUtils.emptyList())
                    .stream()
                    .map(child -> {
                                CommentVO childVO = BeanUtil.copyProperties(child, CommentVO.class);
                                childVO.setAuthor(buildAuthor(child.getUserId()));
                                return childVO;
                            }
                    )
                    .collect(Collectors.toList()));
            voList.add(parentVO);
        }

        return new PageDTO<>(page.getTotal(), page.getPages(), voList);
    }

    /**
     * 删除评论: 评论本人可删自己的; 帖子作者可删自己帖子下的任意评论(含他人)
     * 走MP逻辑删, 查询自动过滤; 顶级评论连带删子评论, 避免孤儿子评论脱离树
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id) {
        Long userId = BaseContext.getCurrentId();
        LifeComment comment = getById(id);
        if (comment == null) {
            throw new ValidationException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            // 非评论本人: 仅帖子作者有管理权
            LifePost post = postMapper.selectById(comment.getPostId());
            if (post == null || !post.getUserId().equals(userId)) {
                throw new ValidationException("无权删除该评论");
            }
        }
        removeById(id);
        remove(new LambdaQueryWrapper<LifeComment>().eq(LifeComment::getParentId, id));
    }

    private AuthorVO buildAuthor(Long userId) {
        LifeUser user = userService.getById(userId);
        return AuthorVO.builder()
                .userId(userId)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .followed(false)
                .build();
    }
}

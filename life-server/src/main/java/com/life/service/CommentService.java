package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.dto.CommentPublishDTO;
import com.life.dto.PageDTO;
import com.life.entity.LifeComment;
import com.life.query.CommentQueryDTO;
import com.life.vo.CommentVO;

public interface CommentService extends IService<LifeComment> {
    void addComment(CommentPublishDTO replyDTO);

    PageDTO<CommentVO> queryCommentPage(CommentQueryDTO query);

    // 删除评论: 本人或帖子作者可删, 顶级评论连带删除其子评论
    void deleteComment(Long id);
}

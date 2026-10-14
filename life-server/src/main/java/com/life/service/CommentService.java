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
}

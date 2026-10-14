package com.life.controller;

import com.life.dto.PageDTO;
import com.life.dto.CommentPublishDTO;
import com.life.query.CommentQueryDTO;
import com.life.service.CommentService;
import com.life.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/replies")
@RequiredArgsConstructor
public class CommentController {


    private final CommentService commentService;

    // 新增评论 或 回复
    @PostMapping
    public void addReply(@RequestBody CommentPublishDTO replyDTO) {
        commentService.addComment(replyDTO);
    }

    // 分页查询评论
    @GetMapping("/page")
    public PageDTO<CommentVO> queryReplyPage(CommentQueryDTO query) {
        return commentService.queryCommentPage(query);
    }

}

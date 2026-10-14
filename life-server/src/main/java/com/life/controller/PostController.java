package com.life.controller;

import com.life.dto.PostPublishDTO;
import com.life.query.PostQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.PostService;
import com.life.vo.PostVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子 C端接口
 */
@RestController
@RequestMapping("/post")
@Slf4j
public class PostController {

    @Autowired
    private PostService postService;

    /**
     * 帖子分页列表: 信息流(type 1-推荐 2-关注) / 指定用户动态(userId) / 关键词(keyword)
     */
    @GetMapping("/page")
    public Result<PageResult> page(PostQueryDTO query) {
        return Result.success(postService.pageQuery(query));
    }

    /**
     * 发布帖子, 返回完整PostVO供前端插入信息流; 成功后自动清理草稿
     */
    @PostMapping("/publish")
    public Result<PostVO> publishPost(@RequestBody PostPublishDTO postDTO) {
        PostVO postVO = postService.publishPost(postDTO);
        return Result.success(postVO);
    }

    /**
     * 打开发布框时拉取上次未发布内容, 无草稿返回data=null(正常态)
     */
    @GetMapping("/draft")
    public Result<PostVO> getDraft() {
        return Result.success(postService.getDraft());
    }

    /**
     * 点X提示"存为草稿"时调用: 一人一份, 有则覆盖无则新建, 返回草稿VO(含id)
     */
    @PutMapping("/draft")
    public Result<PostVO> saveDraft(@RequestBody PostPublishDTO postDTO) {
        return Result.success(postService.saveDraft(postDTO));
    }

    /**
     * 放弃草稿(幂等, 无草稿不报错)
     */
    @DeleteMapping("/draft")
    public Result clearDraft() {
        postService.clearDraft();
        return Result.success();
    }

    /**
     * 删除自己的帖子(逻辑删除)
     */
    @DeleteMapping("/{id}")
    public Result deletePost(@PathVariable Long id) {
        postService.deletePostById(id);
        return Result.success();
    }
}

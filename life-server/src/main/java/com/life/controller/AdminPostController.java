package com.life.controller;

import com.life.query.PostQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2.9 管理端内容审核接口(/admin/** 已由 JwtAdminInterceptor 独立鉴权):
 * 敏感词命中 level2 的帖子落 status=3(审核中), 本接口完成 待审列表 + 通过/驳回;
 * RuoYi 审核页面属 2.13 前端范畴, 接口先行
 */
@RestController
@RequestMapping("/admin/post")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostService postService;

    /**
     * 待审列表: status=3 时间倒序分页
     */
    @GetMapping("/review/page")
    public Result<PageResult> reviewPage(PostQueryDTO query) {
        return Result.success(postService.queryReviewPage(query));
    }

    /**
     * 审核动作: pass=true 通过(3→1, 进信息流+ES索引) / pass=false 驳回(3→4, 不可见+移出索引)
     */
    @PutMapping("/{id}/review")
    public Result review(@PathVariable Long id, @RequestParam boolean pass) {
        postService.reviewPost(id, pass);
        return Result.success(pass ? "已通过" : "已驳回");
    }
}

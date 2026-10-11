package com.life.controller;

import com.life.query.FollowQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifeFollowService;
import com.life.vo.FollowResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/follow")
public class LifeFollowController {

    @Autowired
    private LifeFollowService lifeFollowService;

    /**
     * 关注(幂等, 重复关注直接成功)
     */
    @PostMapping("/{followUserId}")
    public Result<FollowResultVO> follow(@PathVariable Long followUserId) {
        return Result.success(lifeFollowService.follow(followUserId));
    }

    /**
     * 取关
     */
    @DeleteMapping("/{followUserId}")
    public Result<FollowResultVO> unfollow(@PathVariable Long followUserId) {
        return Result.success(lifeFollowService.unfollow(followUserId));
    }

    /**
     * 关注列表: userId不传=本人; total即关注数
     */
    @GetMapping("/following")
    public Result<PageResult> followingList(FollowQueryDTO queryDTO) {
        return Result.success(lifeFollowService.followingList(queryDTO));
    }

    /**
     * 粉丝列表: userId不传=本人; total即粉丝数(被关注者查看自己粉丝数走这里)
     */
    @GetMapping("/followers")
    public Result<PageResult> followersList(FollowQueryDTO queryDTO) {
        return Result.success(lifeFollowService.followersList(queryDTO));
    }

    /**
     * 单查当前登录用户是否已关注TA
     */
    @GetMapping("/{followUserId}/status")
    public Result<Boolean> isFollowed(@PathVariable Long followUserId) {
        return Result.success(lifeFollowService.isFollowed(followUserId));
    }
}

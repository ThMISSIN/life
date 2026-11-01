package com.life.controller;

import com.life.dto.FavoriteToggleDTO;
import com.life.query.FavoriteQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端收藏接口(均需登录, /favorite/** 已注册拦截器)
 */
@RestController
@RequestMapping("/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 收藏/取消 toggle: data=true 收藏成功, data=false 已取消
     */
    @PostMapping
    public Result<Boolean> toggle(@RequestBody FavoriteToggleDTO toggleDTO) {
        return Result.success(favoriteService.toggle(toggleDTO));
    }

    /**
     * 收藏分页(bizType 可选筛选; 目标实时回填, 失效行 valid=false)
     */
    @GetMapping("/page")
    public Result<PageResult> page(FavoriteQueryDTO query) {
        return Result.success(favoriteService.queryFavoritePage(query));
    }

    /**
     * 批量收藏状态(列表页角标): ?bizType=1&ids=1,2,3 -> 已收藏的bizId子集
     */
    @GetMapping("/list")
    public Result<List<Long>> list(@RequestParam Integer bizType, @RequestParam List<Long> ids) {
        return Result.success(favoriteService.listFavoritedBizIds(bizType, ids));
    }
}

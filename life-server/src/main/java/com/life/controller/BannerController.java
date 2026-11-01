package com.life.controller;

import com.life.result.Result;
import com.life.service.BannerService;
import com.life.vo.BannerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C端Banner接口(公开接口, 未注册登录拦截器, 游客可访问)
 */
@RestController
@RequestMapping("/banner")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    /**
     * Banner列表: GET /banner?position=home|benefit|mall (亦支持 1/2/3), 不传查全部
     */
    @GetMapping
    public Result<List<BannerVO>> list(@RequestParam(required = false) String position) {
        return Result.success(bannerService.queryByPosition(position));
    }
}

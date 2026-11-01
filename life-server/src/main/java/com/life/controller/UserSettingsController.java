package com.life.controller;

import com.life.dto.UserSettingsDTO;
import com.life.result.Result;
import com.life.service.UserSettingsService;
import com.life.vo.UserSettingsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C端用户设置接口(均需登录, /user/** 已注册拦截器; 字段归属 life_user_detail)
 */
@RestController
@RequestMapping("/user/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    /**
     * 获取设置(无记录返回默认: 通知开关全开)
     */
    @GetMapping
    public Result<UserSettingsVO> get() {
        return Result.success(userSettingsService.getSettings());
    }

    /**
     * 保存设置(部分更新, 仅覆盖传值字段)
     */
    @PutMapping
    public Result save(@RequestBody UserSettingsDTO settingsDTO) {
        userSettingsService.saveSettings(settingsDTO);
        return Result.success();
    }
}

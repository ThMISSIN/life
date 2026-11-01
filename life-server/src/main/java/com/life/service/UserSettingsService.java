package com.life.service;

import com.life.dto.UserSettingsDTO;
import com.life.vo.UserSettingsVO;

/**
 * 用户设置服务(1.5): 字段归属 life_user_detail, 替换前端本地假保存
 */
public interface UserSettingsService {

    /**
     * 获取当前用户设置(详情行不存在时返回默认值: 开关全开)
     */
    UserSettingsVO getSettings();

    /**
     * 保存当前用户设置(部分更新): 详情行不存在则新建, 开关字段仅接受0/1
     */
    void saveSettings(UserSettingsDTO settingsDTO);
}

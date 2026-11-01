package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.context.BaseContext;
import com.life.dto.UserSettingsDTO;
import com.life.entity.LifeUserDetail;
import com.life.exception.ValidationException;
import com.life.mapper.LifeUserDetailMapper;
import com.life.service.UserSettingsService;
import com.life.utils.BeanUtils;
import com.life.vo.UserSettingsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 用户设置服务实现
 * <p>
 * life_user_detail 与用户 1:1(uk_user_id); 存量用户可能无行(仅管理员seed过id=1),
 * 首次保存时懒创建, 读取时兜默认值, 保证开关语义"默认开启"。
 */
@Service
@Slf4j
public class UserSettingsServiceImpl extends ServiceImpl<LifeUserDetailMapper, LifeUserDetail>
        implements UserSettingsService {

    @Override
    public UserSettingsVO getSettings() {
        LifeUserDetail detail = findByUserId(BaseContext.getCurrentId());
        if (detail == null) {
            return new UserSettingsVO();
        }
        UserSettingsVO vo = BeanUtils.copyBean(detail, UserSettingsVO.class);
        // birthday: LocalDate -> "yyyy-MM-dd" (BeanUtils类型不匹配会跳过, 手动转换)
        if (detail.getBirthday() != null) {
            vo.setBirthday(detail.getBirthday().toString());
        }
        if (vo.getNotifyPoints() == null) {
            vo.setNotifyPoints(1);
        }
        if (vo.getRecommendFeed() == null) {
            vo.setRecommendFeed(1);
        }
        return vo;
    }

    @Override
    public void saveSettings(UserSettingsDTO settingsDTO) {
        Long userId = BaseContext.getCurrentId();
        validateSwitch(settingsDTO.getNotifyPoints(), "积分变动提醒");
        validateSwitch(settingsDTO.getRecommendFeed(), "推荐动态");
        if (settingsDTO.getGender() != null && settingsDTO.getGender() != 0 && settingsDTO.getGender() != 1) {
            throw new ValidationException("性别取值不合法");
        }
        LocalDate birthday = parseBirthday(settingsDTO.getBirthday());

        LifeUserDetail detail = findByUserId(userId);
        boolean isNew = detail == null;
        if (isNew) {
            detail = new LifeUserDetail();
            detail.setUserId(userId);
            detail.setNotifyPoints(1);
            detail.setRecommendFeed(1);
        }
        // 部分更新: 仅覆盖传值字段
        if (settingsDTO.getNotifyPoints() != null) {
            detail.setNotifyPoints(settingsDTO.getNotifyPoints());
        }
        if (settingsDTO.getRecommendFeed() != null) {
            detail.setRecommendFeed(settingsDTO.getRecommendFeed());
        }
        if (settingsDTO.getGender() != null) {
            detail.setGender(settingsDTO.getGender());
        }
        if (birthday != null) {
            detail.setBirthday(birthday);
        }
        if (settingsDTO.getAddress() != null) {
            detail.setAddress(settingsDTO.getAddress());
        }
        if (settingsDTO.getProvince() != null) {
            detail.setProvince(settingsDTO.getProvince());
        }
        if (settingsDTO.getCity() != null) {
            detail.setCity(settingsDTO.getCity());
        }
        if (settingsDTO.getDistrict() != null) {
            detail.setDistrict(settingsDTO.getDistrict());
        }
        if (isNew) {
            save(detail);
        } else {
            updateById(detail);
        }
    }

    private LifeUserDetail findByUserId(Long userId) {
        return this.getOne(new LambdaQueryWrapper<LifeUserDetail>()
                .eq(LifeUserDetail::getUserId, userId));
    }

    private void validateSwitch(Integer value, String label) {
        if (value != null && value != 0 && value != 1) {
            throw new ValidationException(label + "开关取值不合法(0-关闭 1-开启)");
        }
    }

    private LocalDate parseBirthday(String birthday) {
        if (birthday == null || birthday.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(birthday.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("生日格式应为 yyyy-MM-dd");
        }
    }
}

package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.entity.LifeBanner;
import com.life.exception.ValidationException;
import com.life.mapper.LifeBannerMapper;
import com.life.service.BannerService;
import com.life.utils.BeanUtils;
import com.life.vo.BannerVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Banner服务实现
 */
@Service
@Slf4j
public class BannerServiceImpl extends ServiceImpl<LifeBannerMapper, LifeBanner> implements BannerService {

    /**
     * 位置标识 -> 数字码(与前端 mock 语义 home/benefit/mall 对齐)
     */
    private static final Map<String, Integer> POSITION_ALIAS = Map.of(
            "home", 1, "benefit", 2, "mall", 3);

    @Override
    public List<BannerVO> queryByPosition(String position) {
        LambdaQueryWrapper<LifeBanner> wrapper = new LambdaQueryWrapper<LifeBanner>()
                .eq(LifeBanner::getStatus, 1)
                .orderByAsc(LifeBanner::getSort)
                .orderByAsc(LifeBanner::getId);
        if (position != null && !position.isBlank()) {
            Integer code = resolvePosition(position);
            wrapper.eq(LifeBanner::getPosition, code);
        }
        // 生效时间窗: 未配置时间视为长期
        LocalDateTime now = LocalDateTime.now();
        wrapper.and(w -> w.isNull(LifeBanner::getStartTime).or().le(LifeBanner::getStartTime, now))
                .and(w -> w.isNull(LifeBanner::getEndTime).or().ge(LifeBanner::getEndTime, now));

        List<BannerVO> vos = BeanUtils.copyList(list(wrapper), BannerVO.class);
        log.debug("Banner列表查询: position={}, size={}", position, vos.size());
        return vos;
    }

    private Integer resolvePosition(String position) {
        Integer code = POSITION_ALIAS.get(position.toLowerCase());
        if (code != null) {
            return code;
        }
        try {
            int parsed = Integer.parseInt(position);
            if (parsed >= 1 && parsed <= 3) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // 落到统一报错
        }
        throw new ValidationException("Banner位置参数不合法");
    }
}

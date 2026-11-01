package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.alibaba.fastjson.JSON;
import com.life.constant.FavoriteTypeConstant;
import com.life.context.BaseContext;
import com.life.dto.FavoriteToggleDTO;
import com.life.entity.LifeBenefit;
import com.life.entity.LifeFavorite;
import com.life.entity.LifePost;
import com.life.entity.LifeProduct;
import com.life.exception.ValidationException;
import com.life.mapper.LifeBenefitMapper;
import com.life.mapper.LifeFavoriteMapper;
import com.life.mapper.LifePostMapper;
import com.life.mapper.LifeProductMapper;
import com.life.query.FavoriteQueryDTO;
import com.life.result.PageResult;
import com.life.service.FavoriteService;
import com.life.utils.BeanUtils;
import com.life.vo.FavoriteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<LifeFavoriteMapper, LifeFavorite> implements FavoriteService {

    private final LifePostMapper postMapper;
    private final LifeBenefitMapper benefitMapper;
    private final LifeProductMapper productMapper;

    @Override
    public Boolean toggle(FavoriteToggleDTO toggleDTO) {
        Long userId = BaseContext.getCurrentId();
        Integer bizType = toggleDTO.getBizType();
        Long bizId = toggleDTO.getBizId();
        requireSupportedBizType(bizType);
        if (bizId == null) {
            throw new ValidationException("收藏对象不能为空");
        }
        requireTargetExists(bizType, bizId);

        LifeFavorite row = baseMapper.selectAnyByUserBiz(userId, bizId, bizType);
        if (row == null) {
            LifeFavorite favorite = LifeFavorite.builder()
                    .userId(userId)
                    .bizId(bizId)
                    .bizType(bizType)
                    .build();
            try {
                // 公共字段(创建时间/creater/deleted=0)由MP自动填充
                save(favorite);
                return true;
            } catch (DuplicateKeyException e) {
                // 并发双击撞 uk_user_biz: 重查走已有行的toggle分支, 结果幂等
                log.debug("收藏并发唯一键冲突, 转入已有行处理: userId={}, bizId={}, bizType={}", userId, bizId, bizType);
                row = baseMapper.selectAnyByUserBiz(userId, bizId, bizType);
                if (row == null) {
                    throw new ValidationException("收藏操作失败, 请重试");
                }
            }
        }
        if (row.getDeleted() != null && row.getDeleted() == 1) {
            baseMapper.revive(row.getId());
            return true;
        }
        baseMapper.logicDeleteByUserBiz(userId, bizId, bizType);
        return false;
    }

    @Override
    public PageResult queryFavoritePage(FavoriteQueryDTO query) {
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<LifeFavorite> wrapper = new LambdaQueryWrapper<LifeFavorite>()
                .eq(LifeFavorite::getUserId, userId)
                .eq(query.getBizType() != null, LifeFavorite::getBizType, query.getBizType())
                .orderByDesc(LifeFavorite::getCreateTime)
                .orderByDesc(LifeFavorite::getId);
        Page<LifeFavorite> page = this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<FavoriteVO> vos = BeanUtils.copyList(page.getRecords(), FavoriteVO.class);
        // 按类型分组批量回填目标信息(每类型一次IN查询, 防N+1)
        fillTargetInfo(vos);
        return new PageResult(page.getTotal(), vos);
    }

    @Override
    public List<Long> listFavoritedBizIds(Integer bizType, List<Long> bizIds) {
        if (bizIds == null || bizIds.isEmpty()) {
            return Collections.emptyList();
        }
        requireSupportedBizType(bizType);
        Long userId = BaseContext.getCurrentId();
        return this.list(new LambdaQueryWrapper<LifeFavorite>()
                        .select(LifeFavorite::getBizId)
                        .eq(LifeFavorite::getUserId, userId)
                        .eq(LifeFavorite::getBizType, bizType)
                        .in(LifeFavorite::getBizId, bizIds))
                .stream().map(LifeFavorite::getBizId).collect(Collectors.toList());
    }

    // ==================== private ====================

    private void requireSupportedBizType(Integer bizType) {
        if (bizType == null || (!FavoriteTypeConstant.POST.equals(bizType)
                && !FavoriteTypeConstant.BENEFIT.equals(bizType)
                && !FavoriteTypeConstant.GOODS.equals(bizType))) {
            throw new ValidationException("收藏业务类型不支持");
        }
    }

    /**
     * 收藏前置校验目标存在(MP自动过滤逻辑删)
     */
    private void requireTargetExists(Integer bizType, Long bizId) {
        boolean exists;
        if (FavoriteTypeConstant.POST.equals(bizType)) {
            exists = postMapper.selectById(bizId) != null;
        } else if (FavoriteTypeConstant.BENEFIT.equals(bizType)) {
            exists = benefitMapper.selectById(bizId) != null;
        } else {
            exists = productMapper.selectById(bizId) != null;
        }
        if (!exists) {
            throw new ValidationException("收藏对象不存在");
        }
    }

    /**
     * 目标信息回填: 已删除目标不在selectBatchIds结果中 -> valid=false 前端置灰
     */
    private void fillTargetInfo(List<FavoriteVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        Map<Long, LifePost> postMap = batchTarget(vos, FavoriteTypeConstant.POST, postMapper::selectBatchIds, LifePost::getId);
        vos.stream().filter(v -> FavoriteTypeConstant.POST.equals(v.getBizType())).forEach(v -> {
            LifePost post = postMap.get(v.getBizId());
            if (post == null) {
                markInvalid(v);
                return;
            }
            v.setName(firstNonBlank(post.getTitle(), excerpt(post.getContent())));
            v.setImage(firstImage(post.getImages()));
        });
        Map<Long, LifeBenefit> benefitMap = batchTarget(vos, FavoriteTypeConstant.BENEFIT, benefitMapper::selectBatchIds, LifeBenefit::getId);
        vos.stream().filter(v -> FavoriteTypeConstant.BENEFIT.equals(v.getBizType())).forEach(v -> {
            LifeBenefit benefit = benefitMap.get(v.getBizId());
            if (benefit == null) {
                markInvalid(v);
                return;
            }
            v.setName(benefit.getName());
            v.setImage(benefit.getCoverImage());
            v.setPointsPrice(benefit.getPointsPrice());
        });
        Map<Long, LifeProduct> productMap = batchTarget(vos, FavoriteTypeConstant.GOODS, productMapper::selectBatchIds, LifeProduct::getId);
        vos.stream().filter(v -> FavoriteTypeConstant.GOODS.equals(v.getBizType())).forEach(v -> {
            LifeProduct product = productMap.get(v.getBizId());
            if (product == null) {
                markInvalid(v);
                return;
            }
            v.setName(product.getName());
            v.setImage(product.getCoverImage());
            v.setPrice(product.getPrice());
            v.setPointsPrice(product.getPointsPrice());
        });
    }

    private <T> Map<Long, T> batchTarget(List<FavoriteVO> vos, Integer bizType,
                                         Function<List<Long>, List<T>> loader, Function<T, Long> keyFn) {
        List<Long> ids = vos.stream()
                .filter(v -> bizType.equals(v.getBizType()))
                .map(FavoriteVO::getBizId)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<T> targets = loader.apply(new ArrayList<>(ids));
        return targets.stream().collect(Collectors.toMap(keyFn, t -> t));
    }

    private void markInvalid(FavoriteVO v) {
        v.setName("内容已失效");
        v.setValid(false);
    }

    private String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    /**
     * 帖子正文截断做标题兜底(信息流同口径, 80字符)
     */
    private String excerpt(String content) {
        if (content == null) {
            return null;
        }
        return content.length() <= 80 ? content : content.substring(0, 80) + "...";
    }

    /**
     * 帖子首图(images为JSON数组串)
     */
    private String firstImage(String images) {
        if (images == null || images.isBlank()) {
            return null;
        }
        try {
            List<String> list = JSON.parseArray(images, String.class);
            return list == null || list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            return null;
        }
    }
}

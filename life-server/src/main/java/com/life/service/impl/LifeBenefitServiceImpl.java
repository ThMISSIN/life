package com.life.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.BenefitConstant;
import com.life.constant.MessageConstant;
import com.life.constant.UserBenefitStatusConstant;
import com.life.context.BaseContext;
import com.life.entity.LifeBenefit;
import com.life.entity.LifeUser;
import com.life.entity.LifeUserBenefit;
import com.life.exception.ValidationException;
import com.life.mapper.LifeBenefitMapper;
import com.life.mapper.LifeUserBenefitMapper;
import com.life.query.BenefitQueryDTO;
import com.life.result.PageResult;
import com.life.service.LifeBenefitService;
import com.life.vo.BenefitDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LifeBenefitServiceImpl extends ServiceImpl<LifeBenefitMapper, LifeBenefit> implements LifeBenefitService {

    @Autowired
    private LifeUserBenefitMapper lifeUserBenefitMapper;

    @Override
    public PageResult pageList(BenefitQueryDTO query) {
        // C端无论前端传什么, 强制只看上架数据
        query.setStatus(BenefitConstant.LISTED);
        return doPage(query);
    }

    @Override
    public PageResult adminPageList(BenefitQueryDTO query) {
        // status为null查全部(含下架), 由管理端自行筛选
        return doPage(query);
    }

    private PageResult doPage(BenefitQueryDTO query) {
        Page<LifeBenefit> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<LifeBenefit> resultPage = baseMapper.queryList(page, query);
        return new PageResult(resultPage.getTotal(), resultPage.getRecords());
    }

    /**
     * 管理端上下架
     */
    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != BenefitConstant.LISTED && status != BenefitConstant.UNLISTED)) {
            throw new ValidationException("权益状态仅支持: 0-下架, 1-上架");
        }
        LifeBenefit benefit = getById(id);
        if (benefit == null) {
            throw new ValidationException(MessageConstant.BENEFIT_NOT_FOUND);
        }
        LifeBenefit update = new LifeBenefit();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
    }

    /**
     * C端查询权益详情: 仅返回已上架权益, 附带当前用户已兑次数与可兑换标识
     */
    @Override
    public BenefitDetailVO getBenefitDetail(Long id) {
        // 查询所有权益
        LifeBenefit benefit = getById(id);
        // 过滤不存在 以及 未上架 权益
        if (benefit == null || benefit.getStatus() == null || benefit.getStatus() != BenefitConstant.LISTED) {
            throw new ValidationException(MessageConstant.BENEFIT_NOT_FOUND);
        }
        // 查询出用户已兑换次数
        Long myExchangedCount = lifeUserBenefitMapper.selectCount(new LambdaQueryWrapper<LifeUserBenefit>()
                .eq(LifeUserBenefit::getUserId, BaseContext.getCurrentId())
                .eq(LifeUserBenefit::getBenefitId, id)
                .ne(LifeUserBenefit::getStatus, UserBenefitStatusConstant.VOIDED));

        // 是否 限购
        boolean limitReached = benefit.getPerUserLimit() != null
                && benefit.getPerUserLimit() > 0
                && myExchangedCount >= benefit.getPerUserLimit();
        // 是否 库存充足
        boolean stockAvailable = benefit.getStock() != null && benefit.getStock() != 0;
        // 只有 库存充足 且 未达限购限制时, 可兑换
        boolean canExchange = stockAvailable && !limitReached;

        return BenefitDetailVO.builder()
                .id(benefit.getId())
                .name(benefit.getName())
                .categoryId(benefit.getCategoryId())
                .description(benefit.getDescription())
                .coverImage(benefit.getCoverImage())
                .images(parseImages(benefit.getImages()))
                .benefitType(benefit.getBenefitType())
                .pointsPrice(benefit.getPointsPrice())
                .originalPrice(benefit.getOriginalPrice())
                .validType(benefit.getValidType())
                .validDays(benefit.getValidDays())
                .fixedEndTime(benefit.getFixedEndTime())
                .perUserLimit(benefit.getPerUserLimit())
                .minLevel(benefit.getMinLevel())
                .stock(benefit.getStock())
                .sales(benefit.getSales())
                .myExchangedCount(myExchangedCount)
                .canExchange(canExchange)
                .build();
    }

    /**
     * 详情图JSON数组字符串转列表, 数据异常时降级为空列表不影响展示
     */
    private List<String> parseImages(String images) {
        if (images == null || images.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return JSON.parseArray(images, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 管理端新增权益
     */
    @Override
    public void addBenefit(LifeBenefit lifeBenefit) {
        if (lifeBenefit.getName() == null || lifeBenefit.getName().isBlank()) {
            throw new ValidationException(MessageConstant.BENEFIT_NAME_REQUIRED);
        }
        save(lifeBenefit);
    }

    /**
     * 管理端删除权益: 存在待生效/未使用的用户权益时禁止删除, 避免用户持无效券码
     */
    @Override
    public void removeBenefit(Long id) {
        Long count = lifeUserBenefitMapper.selectCount(new LambdaQueryWrapper<LifeUserBenefit>()
                .eq(LifeUserBenefit::getBenefitId, id)
                .in(LifeUserBenefit::getStatus, UserBenefitStatusConstant.PENDING, UserBenefitStatusConstant.UNUSED));
        if (count > 0) {
            throw new ValidationException(MessageConstant.BENEFIT_BE_USED);
        }
        removeById(id);
    }

    /**
     * 兑换资格校验: 权益兑换唯一入口为提交订单权益通道, 校验存在/上架/等级/限购/库存/有效期
     */
    @Override
    public LifeBenefit validateExchange(Long id, int quantity, LifeUser user) {
        // 当权益不存在或者未上架时, 禁止兑换
        LifeBenefit benefit = getById(id);
        if (benefit == null || benefit.getStatus() == null || benefit.getStatus() != BenefitConstant.LISTED) {
            throw new ValidationException(MessageConstant.BENEFIT_NOT_FOUND);
        }
        // 当用户等级不足时, 禁止兑换
        if (benefit.getMinLevel() != null && benefit.getMinLevel() > 0 && user.getLevel() < benefit.getMinLevel()) {
            throw new ValidationException(MessageConstant.USER_LEVEL_NOT_ENOUGH);
        }
        // 已兑换次数(不含作废)+本单数量 达到限制时, 禁止兑换
        if (benefit.getPerUserLimit() != null && benefit.getPerUserLimit() > 0
                && countMyExchange(id, user.getId()) + quantity > benefit.getPerUserLimit()) {
            throw new ValidationException(MessageConstant.USER_EXCHANGED_COUNT_REACH_LIMIT);
        }
        // 判断当前库存是否充足(null为不限量)
        if (benefit.getStock() != null && benefit.getStock() < quantity) {
            throw new ValidationException(MessageConstant.BENEFIT_STOCK_NOT_ENOUGH);
        }
        // 固定截止日的权益已过期的, 禁止兑换(需在扣积分前拦截, 避免发出即过期的券)
        if (benefit.getValidType() != null && benefit.getValidType() == BenefitConstant.VALID_FIXED
                && benefit.getFixedEndTime() != null && !benefit.getFixedEndTime().isAfter(LocalDateTime.now())) {
            throw new ValidationException(MessageConstant.BENEFIT_ALREADY_EXPIRED);
        }
        return benefit;
    }

    /**
     * 扣库存与发放(提交订单权益通道调用): 原子扣库存+销量后按数量逐张生成券码
     */
    @Override
    public List<LifeUserBenefit> grantExchange(LifeBenefit benefit, int quantity, Long userId, String orderNo) {
        // 原子扣库存+加销量(影响行数0=被并发抢完), 不限量(stock为null/<=0)跳过扣减
        if (benefit.getStock() != null && benefit.getStock() > 0
                && baseMapper.deductStock(benefit.getId(), quantity) == 0) {
            throw new ValidationException(MessageConstant.BENEFIT_STOCK_NOT_ENOUGH);
        }
        List<LifeUserBenefit> issued = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            LifeUserBenefit userBenefit = buildUserBenefit(benefit, userId, orderNo);
            lifeUserBenefitMapper.insert(userBenefit);
            issued.add(userBenefit);
        }
        return issued;
    }

    /**
     * 构建用户权益: 快照权益名称/封面, 按有效期类型计算过期时间, 配置缺失时直接拦截
     */
    private LifeUserBenefit buildUserBenefit(LifeBenefit benefit, Long userId, String orderNo) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime;
        // 有效期配置缺失的权益不允许发放, 否则expire_time非空约束会导致发放失败
        if (benefit.getValidType() == null) {
            throw new ValidationException(MessageConstant.BENEFIT_VALID_CONFIG_ERROR);
        } else if (benefit.getValidType() == BenefitConstant.VALID_ROLLING) {
            if (benefit.getValidDays() == null || benefit.getValidDays() <= 0) {
                throw new ValidationException(MessageConstant.BENEFIT_VALID_CONFIG_ERROR);
            }
            expireTime = now.plusDays(benefit.getValidDays());
        } else if (benefit.getValidType() == BenefitConstant.VALID_FIXED) {
            if (benefit.getFixedEndTime() == null) {
                throw new ValidationException(MessageConstant.BENEFIT_VALID_CONFIG_ERROR);
            }
            expireTime = benefit.getFixedEndTime();
        } else {
            throw new ValidationException(MessageConstant.BENEFIT_VALID_CONFIG_ERROR);
        }

        LifeUserBenefit userBenefit = new LifeUserBenefit();
        userBenefit.setUserId(userId);
        userBenefit.setBenefitId(benefit.getId());
        userBenefit.setBenefitName(benefit.getName());
        userBenefit.setCoverImage(benefit.getCoverImage());
        userBenefit.setOrderNo(orderNo);
        // todo 后续升级为自定义券码生成方法(短码, 便于核销输入)
        userBenefit.setCode(UUID.randomUUID().toString().replace("-", ""));
        userBenefit.setEffectiveTime(now);
        userBenefit.setExpireTime(expireTime);
        userBenefit.setStatus(UserBenefitStatusConstant.UNUSED);
        return userBenefit;
    }

    private Long countMyExchange(Long id, Long userId) {
        LambdaQueryWrapper<LifeUserBenefit> eq = new LambdaQueryWrapper<LifeUserBenefit>()
                .eq(LifeUserBenefit::getUserId, userId)
                .eq(LifeUserBenefit::getBenefitId, id)
                .ne(LifeUserBenefit::getStatus, UserBenefitStatusConstant.VOIDED);
        return lifeUserBenefitMapper.selectCount(eq);
    }
}

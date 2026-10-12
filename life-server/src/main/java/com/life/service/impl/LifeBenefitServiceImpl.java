package com.life.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.BenefitConstant;
import com.life.constant.MessageConstant;
import com.life.constant.PointsTypeConstant;
import com.life.constant.StatusConstant;
import com.life.constant.UserBenefitStatusConstant;
import com.life.context.BaseContext;
import com.life.entity.LifeBenefit;
import com.life.entity.LifePointsRecord;
import com.life.entity.LifeUser;
import com.life.entity.LifeUserBenefit;
import com.life.exception.ValidationException;
import com.life.mapper.LifeBenefitMapper;
import com.life.mapper.LifePointsRecordMapper;
import com.life.mapper.LifeUserBenefitMapper;
import com.life.mapper.UserMapper;
import com.life.query.BenefitQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifeBenefitService;
import com.life.service.LifePointsRecordService;
import com.life.vo.BenefitDetailVO;
import com.life.vo.BenefitExchangeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LifeBenefitServiceImpl extends ServiceImpl<LifeBenefitMapper, LifeBenefit> implements LifeBenefitService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private LifeUserBenefitMapper lifeUserBenefitMapper;
    @Autowired
    private LifePointsRecordMapper pointsRecordMapper;

    @Override
    public PageResult pageList(BenefitQueryDTO query) {

        Page<LifeBenefit> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<LifeBenefit> resultPage = baseMapper.queryList(page, query);

        return new PageResult(resultPage.getTotal(), resultPage.getRecords());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    // todo 同一用户并发双击仍可超限兑。 限兑判断是事务前的 selectCount，两个并发请求都能通过。
    //  一期可接受（概率低），后续用 Redis setnx 锁 exchange:{userId}:{benefitId} 解决。
    public BenefitExchangeVO exchangeBenefit(Long id) {
        // 当权益不存在或者未上架时, 禁止兑换
        LifeBenefit benefit = getById(id);
        Long userId = BaseContext.getCurrentId();
        if (benefit == null || benefit.getStatus() == null || benefit.getStatus() != BenefitConstant.LISTED) {
            throw new ValidationException(MessageConstant.BENEFIT_NOT_FOUND);
        }
        // 查询用户
        LifeUser user = userMapper.selectById(userId);
        if (user == null || !StatusConstant.ENABLE.equals(user.getStatus())) {
            throw new ValidationException("用户不存在或已禁用");
        }
        // 当用户等级不足时, 禁止兑换
        if (benefit.getMinLevel() != null && benefit.getMinLevel() > 0 && user.getLevel() < benefit.getMinLevel()) {
            throw new ValidationException(MessageConstant.USER_LEVEL_NOT_ENOUGH);
        }

        // 当用户已兑换次数达限制时, 禁止兑换
        if (benefit.getPerUserLimit() != null && benefit.getPerUserLimit() > 0
                && countMyExchange(id, userId) >= benefit.getPerUserLimit()) {
            throw new ValidationException(MessageConstant.USER_EXCHANGED_COUNT_REACH_LIMIT);
        }

        // 判断当前库存是否充足
        if (benefit.getStock() != null && benefit.getStock() == 0) {
            throw new ValidationException(MessageConstant.BENEFIT_STOCK_NOT_ENOUGH);
        }

        // 固定截止日的权益已过期的, 禁止兑换(需在扣积分前拦截, 避免发出即过期的券)
        if (benefit.getValidType() != null && benefit.getValidType() == BenefitConstant.VALID_FIXED
                && benefit.getFixedEndTime() != null && !benefit.getFixedEndTime().isAfter(LocalDateTime.now())) {
            throw new ValidationException(MessageConstant.BENEFIT_ALREADY_EXPIRED);
        }

        // 扣除积分(影响行数0=不足, 同时写积分流水 type=6)
        if (userMapper.deductPoints(userId, benefit.getPointsPrice()) == 0) {
            throw new ValidationException(MessageConstant.USER_POINTS_NOT_ENOUGH);
        }
        pointsRecordMapper.insert(LifePointsRecord.builder().userId(userId)
                .type(PointsTypeConstant.EXCHANGE).points(-benefit.getPointsPrice()).build());

        // 2. 原子扣库存+加销量(不限量跳过扣减只加sales)
        if (benefit.getStock() != null && benefit.getStock() > 0
                && baseMapper.deductStock(id) == 0) {
            throw new ValidationException(MessageConstant.BENEFIT_STOCK_NOT_ENOUGH);
        }


        // 3. 落库用户权益(快照名称/封面, 计算生效与过期时间)
        // todo 兑换成功通知后续使用rabbitMq异步发送
        LifeUserBenefit userBenefit = buildUserBenefit(benefit, userId);
        lifeUserBenefitMapper.insert(userBenefit);

        // 4. 从落库实体构建VO(insert后主键自动回填, id为用户权益主键)
        return BenefitExchangeVO.builder()
                .id(userBenefit.getId())
                .userId(userId)
                .code(userBenefit.getCode())
                // 后续订单号改为自定义
                .orderNo(String.valueOf(UUID.randomUUID()))
                .effectiveTime(userBenefit.getEffectiveTime())
                .expireTime(userBenefit.getExpireTime())
                .build();
    }

    /**
     * 构建用户权益: 快照权益名称/封面, 按有效期类型计算过期时间, 配置缺失时直接拦截
     */
    private LifeUserBenefit buildUserBenefit(LifeBenefit benefit, Long userId) {
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

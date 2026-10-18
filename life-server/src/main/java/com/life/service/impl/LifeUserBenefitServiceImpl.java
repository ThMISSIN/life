package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.MessageConstant;
import com.life.constant.PointsTypeConstant;
import com.life.constant.UserBenefitStatusConstant;
import com.life.context.BaseContext;
import com.life.entity.LifeBenefit;
import com.life.entity.LifePointsRecord;
import com.life.entity.LifeUserBenefit;
import com.life.exception.ValidationException;
import com.life.mapper.LifeBenefitMapper;
import com.life.mapper.LifePointsRecordMapper;
import com.life.mapper.LifeUserBenefitMapper;
import com.life.mapper.UserMapper;
import com.life.query.UserBenefitQueryDTO;
import com.life.result.PageResult;
import com.life.service.LifeUserBenefitService;
import com.life.utils.BeanUtils;
import com.life.vo.UserBenefitVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LifeUserBenefitServiceImpl extends ServiceImpl<LifeUserBenefitMapper, LifeUserBenefit> implements LifeUserBenefitService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private LifeBenefitMapper benefitMapper;
    @Autowired
    private LifePointsRecordMapper pointsRecordMapper;

    /**
     * C端「我的权益」分页: 仅本人数据, 列表不下发券码
     */
    @Override
    public PageResult queryMyPage(UserBenefitQueryDTO query) {
        LambdaQueryWrapper<LifeUserBenefit> wrapper = new LambdaQueryWrapper<LifeUserBenefit>()
                .eq(LifeUserBenefit::getUserId, BaseContext.getCurrentId())
                .orderByDesc(LifeUserBenefit::getCreateTime);
        applyStatusFilter(wrapper, query.getStatus());
        Page<LifeUserBenefit> page = this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<UserBenefitVO> voList = BeanUtils.copyList(page.getRecords(), UserBenefitVO.class);
        LocalDateTime now = LocalDateTime.now();
        for (UserBenefitVO vo : voList) {
            correctExpiredStatus(vo, now);
            vo.setCode(null);
        }
        return new PageResult(page.getTotal(), voList);
    }

    /**
     * C端「我的权益」详情: 含券码, 供出示核销
     */
    @Override
    public UserBenefitVO getMyDetail(Long id) {
        LifeUserBenefit userBenefit = getOwnById(id);
        UserBenefitVO vo = BeanUtils.copyBean(userBenefit, UserBenefitVO.class);
        correctExpiredStatus(vo, LocalDateTime.now());
        return vo;
    }

    /**
     * C端核销(使用): 校验链 归属->待生效拦截->未使用->未过期
     * 操作人记录当前用户, 后续引入商家角色后改为商家身份并支持按券码核销他人券
     */
    @Override
    public void verifyMyBenefit(Long id) {
        LifeUserBenefit userBenefit = getOwnById(id);
        LocalDateTime now = LocalDateTime.now();
        int status = userBenefit.getStatus() == null ? -1 : userBenefit.getStatus();
        if (status == UserBenefitStatusConstant.PENDING) {
            throw new ValidationException("权益未到生效时间, 暂不能核销");
        }
        if (status != UserBenefitStatusConstant.UNUSED) {
            throw new ValidationException("仅未使用的权益可以核销");
        }
        if (userBenefit.getExpireTime() != null && userBenefit.getExpireTime().isBefore(now)) {
            throw new ValidationException("权益已过期, 不能核销");
        }
        userBenefit.setStatus(UserBenefitStatusConstant.USED);
        userBenefit.setVerifyTime(now);
        userBenefit.setVerifyOperator(BaseContext.getCurrentId());
        updateById(userBenefit);
    }

    /**
     * 管理端发放记录分页: 不限用户, 可按userId/benefitId/orderNo/code筛选, 下发券码
     */
    @Override
    public PageResult queryAdminPage(UserBenefitQueryDTO query) {
        LambdaQueryWrapper<LifeUserBenefit> wrapper = new LambdaQueryWrapper<LifeUserBenefit>()
                .eq(query.getUserId() != null, LifeUserBenefit::getUserId, query.getUserId())
                .eq(query.getBenefitId() != null, LifeUserBenefit::getBenefitId, query.getBenefitId())
                .eq(isNotBlank(query.getOrderNo()), LifeUserBenefit::getOrderNo, query.getOrderNo())
                .eq(isNotBlank(query.getCode()), LifeUserBenefit::getCode, query.getCode())
                .orderByDesc(LifeUserBenefit::getCreateTime);
        applyStatusFilter(wrapper, query.getStatus());
        Page<LifeUserBenefit> page = this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<UserBenefitVO> voList = BeanUtils.copyList(page.getRecords(), UserBenefitVO.class);
        LocalDateTime now = LocalDateTime.now();
        voList.forEach(vo -> correctExpiredStatus(vo, now));
        return new PageResult(page.getTotal(), voList);
    }

    /**
     * 管理端作废: 仅待生效/未使用且未过期的券可作废
     * 三处联动: 券状态->VOIDED(不再计入限兑) | 用户退积分+流水(type=7) | 权益回库存减销量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidUserBenefit(Long id) {
        LifeUserBenefit userBenefit = getById(id);
        if (userBenefit == null) {
            throw new ValidationException("用户权益不存在");
        }
        int status = userBenefit.getStatus() == null ? -1 : userBenefit.getStatus();
        if (status == UserBenefitStatusConstant.VOIDED) {
            throw new ValidationException("该权益已作废");
        }
        if (status == UserBenefitStatusConstant.USED) {
            throw new ValidationException("已核销的权益不能作废");
        }
        boolean expired = (status == UserBenefitStatusConstant.UNUSED
                && userBenefit.getExpireTime() != null
                && userBenefit.getExpireTime().isBefore(LocalDateTime.now()))
                || status == UserBenefitStatusConstant.EXPIRED;
        if (expired) {
            throw new ValidationException("已过期的权益不能作废");
        }
        LifeBenefit benefit = benefitMapper.selectById(userBenefit.getBenefitId());
        if (benefit == null) {
            throw new ValidationException(MessageConstant.BENEFIT_NOT_FOUND);
        }

        // 1. 作废凭证
        userBenefit.setStatus(UserBenefitStatusConstant.VOIDED);
        updateById(userBenefit);

        // 2. 退积分+积分流水(退款额按权益当前积分价, 券上无积分价快照)
        userMapper.addPoints(userBenefit.getUserId(), benefit.getPointsPrice());
        pointsRecordMapper.insert(LifePointsRecord.builder()
                .userId(userBenefit.getUserId())
                .type(PointsTypeConstant.REFUND)
                .points(benefit.getPointsPrice())
                .orderNo(userBenefit.getOrderNo())
                .remark("管理端作废退回: " + userBenefit.getBenefitName())
                .build());

        // 3. 回库存+减销量(不限量口径与扣减一致, 跳过)
        if (benefit.getStock() != null && benefit.getStock() > 0) {
            benefitMapper.backStock(benefit.getId(), 1);
        }
    }

    /**
     * 查询本人权益记录: 不存在/非本人统一报不存在, 避免探测他人单据
     */
    private LifeUserBenefit getOwnById(Long id) {
        LifeUserBenefit userBenefit = getById(id);
        if (userBenefit == null || !userBenefit.getUserId().equals(BaseContext.getCurrentId())) {
            throw new ValidationException("用户权益不存在");
        }
        return userBenefit;
    }

    /**
     * 状态筛选: 过期无落库状态, EXPIRED/UNUSED 由 expireTime 实时区分
     */
    private void applyStatusFilter(LambdaQueryWrapper<LifeUserBenefit> wrapper, Integer status) {
        if (status == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (status == UserBenefitStatusConstant.EXPIRED) {
            wrapper.eq(LifeUserBenefit::getStatus, UserBenefitStatusConstant.UNUSED)
                    .lt(LifeUserBenefit::getExpireTime, now);
        } else if (status == UserBenefitStatusConstant.UNUSED) {
            wrapper.eq(LifeUserBenefit::getStatus, UserBenefitStatusConstant.UNUSED)
                    .ge(LifeUserBenefit::getExpireTime, now);
        } else {
            wrapper.eq(LifeUserBenefit::getStatus, status);
        }
    }

    /**
     * 展示层过期修正: 未使用但已过期的显示为已过期(不落库, 无定时任务)
     */
    private void correctExpiredStatus(UserBenefitVO vo, LocalDateTime now) {
        if (vo.getStatus() != null && vo.getStatus() == UserBenefitStatusConstant.UNUSED
                && vo.getExpireTime() != null && vo.getExpireTime().isBefore(now)) {
            vo.setStatus(UserBenefitStatusConstant.EXPIRED);
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}

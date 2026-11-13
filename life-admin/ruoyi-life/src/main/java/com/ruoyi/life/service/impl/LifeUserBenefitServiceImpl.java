package com.ruoyi.life.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.life.constant.LifeConstants;
import com.ruoyi.life.domain.LifeBenefit;
import com.ruoyi.life.domain.LifePointsRecord;
import com.ruoyi.life.domain.LifeUserBenefit;
import com.ruoyi.life.domain.query.LifeUserBenefitQuery;
import com.ruoyi.life.mapper.LifeBenefitMapper;
import com.ruoyi.life.mapper.LifePointsRecordMapper;
import com.ruoyi.life.mapper.LifeUserBenefitMapper;
import com.ruoyi.life.mapper.LifeUserMapper;
import com.ruoyi.life.service.ILifeUserBenefitService;
import com.ruoyi.life.util.LifeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 用户权益(发放记录)管理 服务实现（业务口径与C端 LifeUserBenefitServiceImpl 管理端方法一致）
 *
 * @author life
 */
@Service
public class LifeUserBenefitServiceImpl implements ILifeUserBenefitService
{
    @Autowired
    private LifeUserBenefitMapper lifeUserBenefitMapper;

    @Autowired
    private LifeBenefitMapper lifeBenefitMapper;

    @Autowired
    private LifePointsRecordMapper lifePointsRecordMapper;

    @Autowired
    private LifeUserMapper lifeUserMapper;

    @Override
    public List<LifeUserBenefit> selectLifeUserBenefitList(LifeUserBenefitQuery query)
    {
        // 时间区间归一化: 仅传日期时 起=00:00:00 止=23:59:59(闭区间含全天)
        query.setBeginTime(LifeUtils.startAt(query.getBeginTime()));
        query.setEndTime(LifeUtils.endAt(query.getEndTime()));
        return lifeUserBenefitMapper.selectLifeUserBenefitList(query);
    }

    /**
     * 管理端作废: 仅待生效/未使用且未过期的券可作废
     * 三处联动: 券状态->VOIDED(不再计入限兑) | 用户退积分+流水(type=7) | 权益回库存减销量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int voidUserBenefit(Long id)
    {
        LifeUserBenefit userBenefit = lifeUserBenefitMapper.selectLifeUserBenefitById(id);
        if (userBenefit == null)
        {
            throw new ServiceException("用户权益不存在");
        }
        int status = userBenefit.getStatus() == null ? -1 : userBenefit.getStatus();
        if (status == LifeConstants.UB_VOIDED)
        {
            throw new ServiceException("该权益已作废");
        }
        if (status == LifeConstants.UB_USED)
        {
            throw new ServiceException("已核销的权益不能作废");
        }
        boolean expired = (status == LifeConstants.UB_UNUSED
                && userBenefit.getExpireTime() != null
                && userBenefit.getExpireTime().before(new Date()))
                || status == LifeConstants.UB_EXPIRED;
        if (expired)
        {
            throw new ServiceException("已过期的权益不能作废");
        }
        LifeBenefit benefit = lifeBenefitMapper.selectLifeBenefitById(userBenefit.getBenefitId());
        if (benefit == null)
        {
            throw new ServiceException("权益不存在");
        }

        // 1. 作废凭证
        LifeUserBenefit update = new LifeUserBenefit();
        update.setId(id);
        update.setStatus(LifeConstants.UB_VOIDED);
        update.setUpdateTime(LifeUtils.now());
        update.setUpdater(LifeUtils.currentOperator());
        lifeUserBenefitMapper.updateLifeUserBenefit(update);

        // 2. 退积分+积分流水(退款额按权益当前积分价, 券上无积分价快照)
        lifeUserMapper.addPoints(userBenefit.getUserId(), benefit.getPointsPrice());
        LifePointsRecord record = new LifePointsRecord();
        record.setUserId(userBenefit.getUserId());
        record.setType(LifeConstants.POINTS_TYPE_REFUND);
        record.setPoints(benefit.getPointsPrice());
        record.setOrderNo(userBenefit.getOrderNo());
        record.setRemark("管理端作废退回: " + userBenefit.getBenefitName());
        record.setCreateTime(LifeUtils.now());
        record.setUpdateTime(LifeUtils.now());
        record.setCreater(LifeUtils.currentOperator());
        record.setUpdater(LifeUtils.currentOperator());
        record.setDeleted(0);
        lifePointsRecordMapper.insertLifePointsRecord(record);

        // 3. 回库存+减销量(不限量口径与扣减一致, 跳过)
        if (benefit.getStock() != null && benefit.getStock() > 0)
        {
            lifeBenefitMapper.backStock(benefit.getId(), 1);
        }
        return 1;
    }

    /**
     * 商家按券码核销(1.7, 校验口径复用C端 verifyMyBenefit):
     * 存在 -> 已生效 -> 未使用 -> 未过期 -> 条件更新置已使用(并发双扫只成功一次);
     * verify_operator 记当前管理端账号, 与C端用户自核销(本人ID)区分商家核销口径
     */
    @Override
    public LifeUserBenefit verifyByCode(String code)
    {
        if (code == null || code.trim().isEmpty())
        {
            throw new ServiceException("券码不能为空");
        }
        LifeUserBenefit userBenefit = lifeUserBenefitMapper.selectByCode(code.trim());
        if (userBenefit == null)
        {
            throw new ServiceException("券码不存在, 请核对后重试");
        }
        int status = userBenefit.getStatus() == null ? -1 : userBenefit.getStatus();
        if (status == LifeConstants.UB_PENDING)
        {
            throw new ServiceException("权益未到生效时间, 暂不能核销");
        }
        if (status == LifeConstants.UB_USED)
        {
            throw new ServiceException("该券已核销, 核销时间: " + com.ruoyi.common.utils.DateUtils.parseDateToStr(
                    com.ruoyi.common.utils.DateUtils.YYYY_MM_DD_HH_MM_SS, userBenefit.getVerifyTime()));
        }
        if (status == LifeConstants.UB_VOIDED)
        {
            throw new ServiceException("该券已作废, 无法核销");
        }
        if (status != LifeConstants.UB_UNUSED)
        {
            throw new ServiceException("仅未使用的权益可以核销");
        }
        if (userBenefit.getExpireTime() != null && userBenefit.getExpireTime().before(new Date()))
        {
            throw new ServiceException("权益已过期, 不能核销");
        }
        int rows = lifeUserBenefitMapper.markVerified(userBenefit.getId(), LifeUtils.now(), LifeUtils.currentOperator());
        if (rows == 0)
        {
            throw new ServiceException("权益状态已变更(并发核销或已被作废), 请刷新后重试");
        }
        userBenefit.setStatus(LifeConstants.UB_USED);
        userBenefit.setVerifyTime(LifeUtils.now());
        userBenefit.setVerifyOperator(LifeUtils.currentOperator());
        return userBenefit;
    }
}

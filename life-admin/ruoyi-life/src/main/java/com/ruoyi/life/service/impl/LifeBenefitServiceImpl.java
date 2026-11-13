package com.ruoyi.life.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.life.constant.LifeConstants;
import com.ruoyi.life.domain.LifeBenefit;
import com.ruoyi.life.domain.query.LifeBenefitQuery;
import com.ruoyi.life.mapper.LifeBenefitCategoryMapper;
import com.ruoyi.life.mapper.LifeBenefitMapper;
import com.ruoyi.life.mapper.LifeUserBenefitMapper;
import com.ruoyi.life.service.ILifeBenefitService;
import com.ruoyi.life.util.LifeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 权益管理 服务实现（业务口径与C端 LifeBenefitServiceImpl 管理端方法一致）
 *
 * @author life
 */
@Service
public class LifeBenefitServiceImpl implements ILifeBenefitService
{
    @Autowired
    private LifeBenefitMapper lifeBenefitMapper;

    @Autowired
    private LifeBenefitCategoryMapper lifeBenefitCategoryMapper;

    @Autowired
    private LifeUserBenefitMapper lifeUserBenefitMapper;

    @Override
    public List<LifeBenefit> selectLifeBenefitList(LifeBenefitQuery query)
    {
        // status为null查全部(含下架), 由管理端自行筛选
        return lifeBenefitMapper.selectLifeBenefitList(query);
    }

    @Override
    public LifeBenefit selectLifeBenefitById(Long id)
    {
        return lifeBenefitMapper.selectLifeBenefitById(id);
    }

    /**
     * 管理端新增权益: 校验名称
     */
    @Override
    public int insertLifeBenefit(LifeBenefit lifeBenefit)
    {
        if (lifeBenefit.getName() == null || lifeBenefit.getName().isBlank())
        {
            throw new ServiceException("权益名称不能为空");
        }
        lifeBenefit.setCreateTime(LifeUtils.now());
        lifeBenefit.setUpdateTime(LifeUtils.now());
        lifeBenefit.setCreater(LifeUtils.currentOperator());
        lifeBenefit.setUpdater(LifeUtils.currentOperator());
        lifeBenefit.setDeleted(0);
        return lifeBenefitMapper.insertLifeBenefit(lifeBenefit);
    }

    /**
     * 管理端修改权益
     */
    @Override
    public int updateLifeBenefit(LifeBenefit lifeBenefit)
    {
        if (lifeBenefit.getId() == null)
        {
            throw new ServiceException("权益ID不能为空");
        }
        if (lifeBenefitMapper.selectLifeBenefitById(lifeBenefit.getId()) == null)
        {
            throw new ServiceException("权益不存在");
        }
        lifeBenefit.setUpdateTime(LifeUtils.now());
        lifeBenefit.setUpdater(LifeUtils.currentOperator());
        return lifeBenefitMapper.updateLifeBenefit(lifeBenefit);
    }

    /**
     * 管理端上下架
     */
    @Override
    public int changeStatus(Long id, Integer status)
    {
        if (status == null || (status != LifeConstants.LISTED && status != LifeConstants.UNLISTED))
        {
            throw new ServiceException("权益状态仅支持: 0-下架, 1-上架");
        }
        if (lifeBenefitMapper.selectLifeBenefitById(id) == null)
        {
            throw new ServiceException("权益不存在");
        }
        LifeBenefit update = new LifeBenefit();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(LifeUtils.now());
        update.setUpdater(LifeUtils.currentOperator());
        return lifeBenefitMapper.updateLifeBenefit(update);
    }

    /**
     * 管理端删除权益: 存在待生效/未使用的发放记录时禁止删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteLifeBenefitByIds(Long[] ids)
    {
        int count = 0;
        for (Long id : ids)
        {
            LifeBenefit benefit = lifeBenefitMapper.selectLifeBenefitById(id);
            if (benefit == null)
            {
                throw new ServiceException("权益不存在");
            }
            int activeCount = lifeUserBenefitMapper.countActiveByBenefitId(id);
            if (activeCount > 0)
            {
                throw new ServiceException("权益[" + benefit.getName() + "]存在未使用的发放记录, 禁止删除");
            }
            count += lifeBenefitMapper.deleteLifeBenefitById(id);
        }
        return count;
    }
}

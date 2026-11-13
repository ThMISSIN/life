package com.ruoyi.life.mapper;

import com.ruoyi.life.domain.LifeUserBenefit;
import com.ruoyi.life.domain.query.LifeUserBenefitQuery;

import java.util.Date;
import java.util.List;

/**
 * 用户权益(发放记录) 数据层
 */
public interface LifeUserBenefitMapper
{
    /** 管理端分页查询(关联用户昵称, 时间降序) */
    public List<LifeUserBenefit> selectLifeUserBenefitList(LifeUserBenefitQuery query);

    /** 按ID查询 */
    public LifeUserBenefit selectLifeUserBenefitById(Long id);

    /** 按券码查询(商家核销前凭证查询, 关联用户昵称) */
    public LifeUserBenefit selectByCode(String code);

    /** 修改(作废) */
    public int updateLifeUserBenefit(LifeUserBenefit lifeUserBenefit);

    /** 核销: 条件更新 未使用(1)->已使用(2), 并发双击只生效一次 */
    public int markVerified(Long id, Date verifyTime, Long verifyOperator);

    /** 某权益下 待生效/未使用 的发放数(删除权益前校验) */
    public int countActiveByBenefitId(Long benefitId);
}

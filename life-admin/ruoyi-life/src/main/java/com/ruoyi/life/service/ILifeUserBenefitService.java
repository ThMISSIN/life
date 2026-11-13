package com.ruoyi.life.service;

import com.ruoyi.life.domain.LifeUserBenefit;
import com.ruoyi.life.domain.query.LifeUserBenefitQuery;

import java.util.List;

/**
 * 用户权益(发放记录)管理 服务接口（迁移自C端 LifeUserBenefitService 管理端方法）
 */
public interface ILifeUserBenefitService
{
    /** 分页查询: 可按userId/benefitId/status/orderNo/code/权益名/用户关键词/时间区间筛选, 含券码 */
    public List<LifeUserBenefit> selectLifeUserBenefitList(LifeUserBenefitQuery query);

    /** 作废发放记录: 券作废 + 退积分(写流水) + 回库存减销量 */
    public int voidUserBenefit(Long id);

    /**
     * 商家按券码核销(1.7): 校验存在/未使用/已生效/未过期后置已使用,
     * 核销操作人记当前管理端账号(对应 verify_operator 商家口径)
     *
     * @return 核销成功后的发放记录(供小票/回执展示)
     */
    public LifeUserBenefit verifyByCode(String code);
}

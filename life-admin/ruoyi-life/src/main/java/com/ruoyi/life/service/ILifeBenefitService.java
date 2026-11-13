package com.ruoyi.life.service;

import com.ruoyi.life.domain.LifeBenefit;
import com.ruoyi.life.domain.query.LifeBenefitQuery;

import java.util.List;

/**
 * 权益管理 服务接口（迁移自C端 LifeBenefitService 管理端方法）
 */
public interface ILifeBenefitService
{
    /** 分页查询(status不传查全部, 含下架) */
    public List<LifeBenefit> selectLifeBenefitList(LifeBenefitQuery query);

    /** 按ID查询 */
    public LifeBenefit selectLifeBenefitById(Long id);

    /** 新增权益 */
    public int insertLifeBenefit(LifeBenefit lifeBenefit);

    /** 修改权益 */
    public int updateLifeBenefit(LifeBenefit lifeBenefit);

    /** 上下架 */
    public int changeStatus(Long id, Integer status);

    /** 删除权益(软删除, 存在待生效/未使用发放记录时禁止) */
    public int deleteLifeBenefitByIds(Long[] ids);
}

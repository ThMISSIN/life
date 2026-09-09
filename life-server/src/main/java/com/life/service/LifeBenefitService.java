package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.entity.LifeBenefit;
import com.life.query.BenefitQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.vo.BenefitDetailVO;
import com.life.vo.BenefitExchangeVO;

public interface LifeBenefitService extends IService<LifeBenefit> {

    /**
     * C端分页查询已上架权益
     */
    PageResult pageList(BenefitQueryDTO query);

    /**
     * C端查询权益详情(含当前用户已兑次数与可兑换标识)
     */
    BenefitDetailVO getBenefitDetail(Long id);

    /**
     * 管理端新增权益
     */
    void addBenefit(LifeBenefit lifeBenefit);

    /**
     * 管理端删除权益(软删除)
     */
    void removeBenefit(Long id);

    BenefitExchangeVO exchangeBenefit(Long id);
}

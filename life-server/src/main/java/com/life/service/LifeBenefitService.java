package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.entity.LifeBenefit;
import com.life.entity.LifeUser;
import com.life.entity.LifeUserBenefit;
import com.life.query.BenefitQueryDTO;
import com.life.result.PageResult;
import com.life.vo.BenefitDetailVO;

import java.util.List;

public interface LifeBenefitService extends IService<LifeBenefit> {

    /**
     * C端分页查询已上架权益
     */
    PageResult pageList(BenefitQueryDTO query);

    /**
     * 管理端分页查询(可见下架数据, status不传查全部)
     */
    PageResult adminPageList(BenefitQueryDTO query);

    /**
     * 管理端上下架
     */
    void updateStatus(Long id, Integer status);

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

    /**
     * 兑换资格校验(权益兑换唯一入口为提交订单权益通道):
     * 存在/上架/等级/限购(已兑不含作废+本次数量)/库存(null为不限量)/固定截止日未过期
     */
    LifeBenefit validateExchange(Long id, int quantity, LifeUser user);

    /**
     * 扣库存与发放(提交订单权益通道调用): 原子扣库存+销量, 按数量逐张生成券码
     *
     * @param orderNo 凭证单号, 即真实订单号
     */
    List<LifeUserBenefit> grantExchange(LifeBenefit benefit, int quantity, Long userId, String orderNo);
}

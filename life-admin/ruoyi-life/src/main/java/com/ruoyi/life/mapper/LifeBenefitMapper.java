package com.ruoyi.life.mapper;

import com.ruoyi.life.domain.LifeBenefit;
import com.ruoyi.life.domain.query.LifeBenefitQuery;

import java.util.List;

/**
 * 权益 数据层
 */
public interface LifeBenefitMapper
{
    /** 管理端分页查询(status不传查全部, 含下架) */
    public List<LifeBenefit> selectLifeBenefitList(LifeBenefitQuery query);

    /** 按ID查询 */
    public LifeBenefit selectLifeBenefitById(Long id);

    /** 新增 */
    public int insertLifeBenefit(LifeBenefit lifeBenefit);

    /** 修改 */
    public int updateLifeBenefit(LifeBenefit lifeBenefit);

    /** 逻辑删除 */
    public int deleteLifeBenefitById(Long id);

    /** 回库存+减销量(作废退回, 口径与C端 backStock 一致) */
    public int backStock(Long id, Integer quantity);

    /** 分类下权益数(删除分类前校验) */
    public int countByCategoryId(Long categoryId);
}

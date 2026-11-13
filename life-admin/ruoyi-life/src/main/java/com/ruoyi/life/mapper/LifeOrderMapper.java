package com.ruoyi.life.mapper;

import com.ruoyi.life.domain.LifeOrder;
import com.ruoyi.life.domain.query.LifeOrderQuery;

import java.util.List;

/**
 * 订单 数据层
 */
public interface LifeOrderMapper
{
    /** 管理端分页查询(关联下单人昵称, 时间降序) */
    public List<LifeOrder> selectLifeOrderList(LifeOrderQuery query);

    /** 按ID查询 */
    public LifeOrder selectLifeOrderById(Long id);

    /** 修改(发货) */
    public int updateLifeOrder(LifeOrder lifeOrder);
}

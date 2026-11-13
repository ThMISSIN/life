package com.ruoyi.life.service;

import com.ruoyi.life.domain.LifeOrder;
import com.ruoyi.life.domain.query.LifeOrderQuery;

import java.util.List;

/**
 * 订单管理 服务接口（迁移自C端 OrderService 管理端方法）
 */
public interface ILifeOrderService
{
    /** 分页查询(关联下单人昵称+订单明细) */
    public List<LifeOrder> selectLifeOrderList(LifeOrderQuery query);

    /** 订单详情(含明细) */
    public LifeOrder selectLifeOrderById(Long id);

    /** 发货(仅已支付订单) */
    public int deliverOrder(Long id);
}

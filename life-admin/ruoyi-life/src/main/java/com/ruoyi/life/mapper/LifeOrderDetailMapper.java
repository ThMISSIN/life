package com.ruoyi.life.mapper;

import com.ruoyi.life.domain.LifeOrderDetail;

import java.util.List;

/**
 * 订单明细 数据层
 */
public interface LifeOrderDetailMapper
{
    /** 按订单ID集合批量查询明细 */
    public List<LifeOrderDetail> selectByOrderIds(List<Long> orderIds);

    /** 按订单号查询明细 */
    public List<LifeOrderDetail> selectByOrderNo(String orderNo);
}

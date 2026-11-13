package com.ruoyi.life.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.life.constant.LifeConstants;
import com.ruoyi.life.domain.LifeOrder;
import com.ruoyi.life.domain.LifeOrderDetail;
import com.ruoyi.life.domain.query.LifeOrderQuery;
import com.ruoyi.life.mapper.LifeOrderDetailMapper;
import com.ruoyi.life.mapper.LifeOrderMapper;
import com.ruoyi.life.service.ILifeOrderService;
import com.ruoyi.life.util.LifeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单管理 服务实现（业务口径与C端 OrderServiceImpl 管理端方法一致）
 *
 * @author life
 */
@Service
public class LifeOrderServiceImpl implements ILifeOrderService
{
    @Autowired
    private LifeOrderMapper lifeOrderMapper;

    @Autowired
    private LifeOrderDetailMapper lifeOrderDetailMapper;

    @Override
    public List<LifeOrder> selectLifeOrderList(LifeOrderQuery query)
    {
        query.setStartTime(LifeUtils.startAt(query.getStartTime()));
        query.setEndTime(LifeUtils.endAt(query.getEndTime()));
        List<LifeOrder> orders = lifeOrderMapper.selectLifeOrderList(query);
        fillDetails(orders);
        return orders;
    }

    @Override
    public LifeOrder selectLifeOrderById(Long id)
    {
        LifeOrder order = lifeOrderMapper.selectLifeOrderById(id);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }
        List<LifeOrderDetail> details = lifeOrderDetailMapper.selectByOrderNo(order.getOrderNo());
        order.setDetails(details);
        return order;
    }

    /**
     * 发货: 仅已支付订单(口径与C端 deliverOrder 一致)
     */
    @Override
    public int deliverOrder(Long id)
    {
        LifeOrder order = lifeOrderMapper.selectLifeOrderById(id);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != LifeConstants.ORDER_PAID)
        {
            throw new ServiceException("订单已支付才可以发货");
        }
        LifeOrder update = new LifeOrder();
        update.setId(id);
        update.setStatus(LifeConstants.ORDER_DELIVERED);
        update.setUpdateTime(LifeUtils.now());
        update.setUpdater(LifeUtils.currentOperator());
        return lifeOrderMapper.updateLifeOrder(update);
    }

    /** 批量补齐订单明细, 避免逐行查 */
    private void fillDetails(List<LifeOrder> orders)
    {
        if (orders == null || orders.isEmpty())
        {
            return;
        }
        List<Long> orderIds = new ArrayList<>();
        for (LifeOrder order : orders)
        {
            orderIds.add(order.getId());
        }
        List<LifeOrderDetail> details = lifeOrderDetailMapper.selectByOrderIds(orderIds);
        for (LifeOrder order : orders)
        {
            List<LifeOrderDetail> own = new ArrayList<>();
            for (LifeOrderDetail detail : details)
            {
                if (detail.getOrderId().equals(order.getId()))
                {
                    own.add(detail);
                }
            }
            order.setDetails(own);
        }
    }
}

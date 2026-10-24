package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.dto.OrderSubmitDTO;
import com.life.dto.PageDTO;
import com.life.entity.LifeOrder;
import com.life.query.OrderQueryDTO;
import com.life.result.PageResult;
import com.life.vo.OrderVO;

public interface OrderService extends IService<LifeOrder> {

    /**
     * 提交订单(扣减积分/库存、写订单与明细)
     */
    OrderVO submitOrder(OrderSubmitDTO submitDTO);

    /**
     * 我的订单分页(按状态筛选)
     */
    PageDTO<OrderVO> queryMyOrderPage(OrderQueryDTO query);

    /**
     * 订单详情(带明细)
     */
    OrderVO getOrderDetail(Long id);

    /**
     * 用户取消订单(仅待支付可取消)
     */
    void cancelOrder(Long id);

    /**
     * 支付订单(一期模拟支付: 余额通道同步成功, 仅待支付可支付)
     */
    void payOrder(Long id);

    /**
     * 管理端: 订单分页
     */
    PageResult queryAdminOrderPage(OrderQueryDTO query);

    /**
     * 管理端: 发货(仅已支付可发货)
     */
    void deliverOrder(Long id);

    /**
     * 确认收货(仅本人、仅已发货, 状态 2→3, 商品单状态机最后一环)
     */
    void confirmOrder(Long id);

}

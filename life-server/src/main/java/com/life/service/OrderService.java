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
    /**
     * 签发下单幂等令牌: Redisson bucket 写 Redis 初始状态 INIT(TTL 30min), 提交订单时原子流转 INIT->USED;
     * 下单成功令牌永久失效, 下单失败还原 INIT 可用同令牌重试
     */
    String generateOrderToken();

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

    /**
     * 超时自动确认收货(定时任务入口, 无登录态): 已发货且 update_time 超 N 天 → 批量置3, 返回处理单数
     */
    int autoConfirmTimeoutOrders();

    /**
     * 2.5 管理端退款(已支付/已发货实物单): 退款单落库(uk幂等) + 订单条件置5(CAS防双退)
     * + 余额/积分原路退回(changePoints bizNo=RF:{orderNo}) + 库存回退(DB+Redis镜像) + MQ通知用户
     */
    void refundOrder(Long id, String reason);

    /**
     * 2.5/2.12 超时关单幂等入口(延迟消息消费与扫描兜底任务共用):
     * 条件更新 status 0→4(已支付/已取消自然0行跳过) + 回库存 + 解冻抵扣积分
     *
     * @return true=本次执行了关单, false=订单已非待支付(无需处理)
     */
    boolean closeOrderIfPending(Long orderId);

    /**
     * 2.5 扫描兜底关单(定时任务入口): status=0 且 create_time 超过 life.order.auto-close-minutes
     * 的实物单逐单走 closeOrderIfPending; 返回处理单数
     */
    int closeTimeoutOrders();

}

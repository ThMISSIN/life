package com.life.controller;

import com.life.dto.OrderSubmitDTO;
import com.life.dto.PageDTO;
import com.life.query.OrderQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.OrderService;
import com.life.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * C端订单接口(均需登录, /order/** 已注册拦截器)
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 提交订单
     */
    @PostMapping
    public Result<OrderVO> submit(@RequestBody OrderSubmitDTO submitDTO) {
        return Result.success(orderService.submitOrder(submitDTO));
    }

    /**
     * 我的订单分页(按状态筛选)
     */
    @GetMapping("/page")
    public Result<PageDTO<OrderVO>> page(OrderQueryDTO query) {
        return Result.success(orderService.queryMyOrderPage(query));
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(id));
    }

    /**
     * 取消订单(仅待支付)
     */
    @PutMapping("/{id}/cancel")
    public Result cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success();
    }

    /**
     * 支付订单(一期模拟支付: 余额通道同步成功, 仅待支付可支付)
     */
    @PutMapping("/{id}/pay")
    public Result pay(@PathVariable Long id) {
        orderService.payOrder(id);
        return Result.success();
    }
}

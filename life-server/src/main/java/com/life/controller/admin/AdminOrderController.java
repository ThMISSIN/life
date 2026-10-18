package com.life.controller.admin;

import com.life.query.OrderQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端订单接口(挂 /admin/**, 已在拦截器内)
 */
@RestController
@RequestMapping("/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * 订单分页
     */
    @GetMapping("/page")
    public Result<PageResult> page(OrderQueryDTO query) {
        return Result.success(orderService.queryAdminOrderPage(query));
    }

    /**
     * 发货(仅已支付订单)
     */
    @PutMapping("/{id}/deliver")
    public Result deliver(@PathVariable Long id) {
        orderService.deliverOrder(id);
        return Result.success();
    }
}

package com.life.controller;

import com.life.dto.RefundDTO;
import com.life.exception.ValidationException;
import com.life.result.Result;
import com.life.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2.5 管理端订单退款(/admin/** 独立鉴权): 已支付/已发货实物单 →
 * 退款单落库 + 余额/积分原路退回 + 库存回退 + 订单置5 + MQ通知用户;
 * 权益兑换单不走本口(按张作废见 RuoYi /life/userBenefit/void)
 */
@RestController
@RequestMapping("/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * 退款: PUT /admin/order/{id}/refund  body {"reason": "..."}
     */
    @PutMapping("/{id}/refund")
    public Result refund(@PathVariable Long id, @RequestBody(required = false) RefundDTO dto) {
        String reason = dto == null || dto.getReason() == null || dto.getReason().isBlank()
                ? null : dto.getReason().trim();
        if (reason == null) {
            throw new ValidationException("请填写退款原因");
        }
        orderService.refundOrder(id, reason);
        return Result.success("退款成功");
    }
}

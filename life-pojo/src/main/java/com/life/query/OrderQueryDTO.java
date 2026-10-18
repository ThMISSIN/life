package com.life.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderQueryDTO extends QueryDTO {

    /**
     * 订单状态: 0-待支付, 1-已支付, 2-已发货, 3-已完成, 4-已取消, 5-已退款; 不传查全部
     */
    private Integer status;

    /**
     * 订单类型: 1-商品订单, 2-权益订单; 不传查全部
     */
    private Integer orderType;

    /**
     * 用户ID, 仅管理端筛选生效, C端强制当前登录用户
     */
    private Long userId;
}

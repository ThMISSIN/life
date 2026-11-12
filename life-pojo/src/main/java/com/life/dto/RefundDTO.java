package com.life.dto;

import lombok.Data;

/**
 * 2.5 管理端退款请求体
 */
@Data
public class RefundDTO {

    /** 退款原因(必填, 落 life_refund_order.reason 留痕) */
    private String reason;
}

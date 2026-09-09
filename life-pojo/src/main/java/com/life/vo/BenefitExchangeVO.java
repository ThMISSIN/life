package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenefitExchangeVO {

    private Long id;

    // 用户id
    private Long userId;

    // 订单编号
    private String orderNo;

    // 商品条码
    private String code;

    // 生效时间
    private LocalDateTime effectiveTime;

    // 过期时间
    private LocalDateTime expireTime;




}

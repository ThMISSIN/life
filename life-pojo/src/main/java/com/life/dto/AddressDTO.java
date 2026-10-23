package com.life.dto;

import lombok.Data;

/**
 * C端收货地址DTO(新增/修改共用)
 */
@Data
public class AddressDTO {

    /**
     * 地址ID(修改必传, 新增不传)
     */
    private Long id;

    /**
     * 收货人姓名
     */
    private String receiverName;

    /**
     * 收货人手机号
     */
    private String receiverPhone;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区
     */
    private String district;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 是否默认: 0-否, 1-是; 修改时null表示不变更
     */
    private Integer isDefault;
}

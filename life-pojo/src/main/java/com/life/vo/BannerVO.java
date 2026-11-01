package com.life.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * C端Banner展示VO(仅下发前端需要的字段, 不含审计/时间窗配置)
 */
@Data
public class BannerVO implements Serializable {

    private Long id;

    private String title;

    /**
     * 图片URL
     */
    private String image;

    /**
     * 跳转链接
     */
    private String url;

    /**
     * 业务类型: 1-商品, 2-权益, 3-活动(可空, 前端可据此跳详情)
     */
    private Integer bizType;

    /**
     * 业务ID(可空)
     */
    private Long bizId;
}

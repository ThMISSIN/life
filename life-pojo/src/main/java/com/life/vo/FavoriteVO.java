package com.life.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏列表项VO(目标信息按 bizType 从对应业务表实时回填)
 */
@Data
public class FavoriteVO implements Serializable {

    private Long id;

    /**
     * 业务类型: 1-帖子, 2-权益, 3-商品
     */
    private Integer bizType;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * 目标名称(帖子标题/权益名/商品名), 目标已删除时为占位文案
     */
    private String name;

    /**
     * 目标封面图(帖子取首图)
     */
    private String image;

    /**
     * 现金价(分, 仅商品)
     */
    private Integer price;

    /**
     * 积分价(仅权益/商品)
     */
    private Integer pointsPrice;

    /**
     * 目标是否仍存在(未删除); false 时前端置灰并禁止跳转(默认true)
     */
    private Boolean valid = true;

    /**
     * 收藏时间
     */
    private LocalDateTime createTime;
}

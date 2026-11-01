package com.life.dto;

import lombok.Data;

/**
 * 收藏/取消 toggle 入参(POST /favorite)
 */
@Data
public class FavoriteToggleDTO {

    /**
     * 业务ID(帖子/权益/商品ID)
     */
    private Long bizId;

    /**
     * 业务类型: 1-帖子, 2-权益, 3-商品
     */
    private Integer bizType;
}

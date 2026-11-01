package com.life.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收藏分页查询DTO(GET /favorite/page)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FavoriteQueryDTO extends QueryDTO {

    /**
     * 业务类型: 1-帖子, 2-权益, 3-商品; 不传查全部
     */
    private Integer bizType;
}

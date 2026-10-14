package com.life.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductQueryDTO extends QueryDTO {

    /**
     * 分类ID, 传一级分类ID时只返回挂在该分类下的商品
     */
    private Long categoryId;

    /**
     * 关键词, 按商品名称/店铺名模糊搜索
     */
    private String keyword;

    /**
     * 排序方式: 1-综合(默认,按sort), 2-销量, 3-价格升序, 4-价格降序
     */
    private Integer sortType;

    /**
     * 状态: 0-下架, 1-上架; 仅管理端可传, C端由服务层强制为上架
     */
    private Integer status;
}

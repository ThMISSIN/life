package com.life.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权益分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BenefitQueryDTO extends QueryDTO {

    /**
     * 权益状态: 0-下架, 1-上架; 管理端不传查全部, C端由服务层强制为1
     */
    private Integer status;

    /**
     * 分类ID, 传一级分类ID时只返回挂在该分类下的权益
     */
    private Long categoryId;

    /**
     * 关键词, 按权益名称模糊搜索
     */
    private String keyword;

    /**
     * 排序方式: 1-综合(默认,按sort), 2-销量, 3-积分价格升序, 4-积分价格降序
     */
    private Integer sortType;
}

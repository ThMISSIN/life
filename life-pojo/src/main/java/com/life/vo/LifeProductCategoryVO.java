package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品分类树VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifeProductCategoryVO {

    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类图标
     */
    private String icon;

    // 二级子分类
    private List<LifeProductCategoryVO> children;

}

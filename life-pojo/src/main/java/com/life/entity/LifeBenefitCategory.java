package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权益分类表
 */
@Data
@TableName("life_benefit_category")
public class LifeBenefitCategory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类图标
     */
    private String icon;

    /**
     * 父分类ID, 0表示顶级分类
     */
    private Long parentId;

    /**
     * 分类层级: 1-一级, 2-二级
     */
    private Integer level;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态: 0-禁用, 1-正常
     */
    private Integer status;
}

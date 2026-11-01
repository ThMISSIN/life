package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 轮播图表(life_banner)
 * <p>
 * position 为 MySQL 保留字, 列名映射内嵌反引号; 展示位置与 biz_type(内容维度)是两个维度。
 */
@Data
@TableName("life_banner")
public class LifeBanner extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
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
     * 业务类型: 1-商品, 2-权益, 3-活动
     */
    private Integer bizType;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * 展示位置: 1-首页, 2-权益中心, 3-商城
     */
    @TableField(value = "`position`")
    private Integer position;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态: 0-禁用, 1-正常
     */
    private Integer status;

    /**
     * 开始时间(空=不限)
     */
    private LocalDateTime startTime;

    /**
     * 结束时间(空=不限)
     */
    private LocalDateTime endTime;
}

package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 敏感词(2.9/3.6): DFA 过滤数据源, 服务内存缓存 + 定时刷新
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_sensitive_word")
public class LifeSensitiveWord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 敏感词(唯一键 uk_word) */
    private String word;

    /** 分类: 涉政/涉黄/赌博/广告/辱骂/其他 */
    private String category;

    /** 命中策略: 1-拒绝发布, 2-转人工审核, 3-替换*** */
    private Integer level;

    /** 状态: 0-停用, 1-启用 */
    private Integer status;
}

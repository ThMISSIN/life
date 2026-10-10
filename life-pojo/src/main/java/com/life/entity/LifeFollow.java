package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 关注表
 */
@Data
@TableName("life_follow")
public class LifeFollow extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID(关注发起人)
     */
    private Long userId;

    /**
     * 被关注用户ID
     */
    private Long followUserId;
}

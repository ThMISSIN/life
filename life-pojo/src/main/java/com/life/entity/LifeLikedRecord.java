package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 点赞记录表
 */
@Data
@TableName("life_liked_record")
public class LifeLikedRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * 业务类型: 1-帖子, 2-评论, 3-权益, 4-商品, 5-活动
     */
    private Integer bizType;
}

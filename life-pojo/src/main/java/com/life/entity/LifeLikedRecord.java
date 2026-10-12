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
     * 业务类型: POST-帖子, COMMENT-评论, BENEFIT-权益, GOODS-商品, ACTIVITY-活动
     */
    private String bizType;
}

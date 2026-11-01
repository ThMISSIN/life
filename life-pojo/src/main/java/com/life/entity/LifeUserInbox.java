package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户收件箱表(站内通知落库, 一行=一条用户通知)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_user_inbox")
public class LifeUserInbox extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收用户ID
     */
    private Long userId;

    /**
     * 通知类型: 1-系统通知, 2-订单通知, 3-活动通知, 4-权益通知, 5-积分通知
     */
    private Integer type;

    /**
     * 通知标题(模板渲染后的最终文案)
     */
    private String title;

    /**
     * 通知内容(模板渲染后的最终文案)
     */
    private String content;

    /**
     * 业务ID(如订单ID/公告ID, 前端按类型跳转用)
     */
    private Long bizId;

    /**
     * 跳转链接
     */
    private String url;

    /**
     * 已读标志: 0-未读, 1-已读
     */
    private Integer isRead;
}

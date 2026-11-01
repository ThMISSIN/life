package com.life.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * C端通知列表项VO(收件箱对用户的展示视图)
 */
@Data
public class NoticeVO implements Serializable {

    private Long id;

    /**
     * 通知类型: 1-系统通知, 2-订单通知, 3-活动通知, 4-权益通知, 5-积分通知
     */
    private Integer type;

    private String title;

    private String content;

    /**
     * 业务ID
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

    private LocalDateTime createTime;
}

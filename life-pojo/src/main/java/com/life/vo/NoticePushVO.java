package com.life.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * WebSocket 推送消息体(服务端->客户端)
 * event: connected-握手回执, notice-新通知, unread-未读数变更
 */
@Data
@Builder
public class NoticePushVO implements Serializable {

    /**
     * 事件类型: connected / notice / unread
     */
    private String event;

    /**
     * 通知内容(event=notice时携带)
     */
    private NoticeVO notice;

    /**
     * 当前未读总数(始终携带, 前端直接刷角标)
     */
    private Long unreadCount;
}

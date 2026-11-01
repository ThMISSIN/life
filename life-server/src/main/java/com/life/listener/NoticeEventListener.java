package com.life.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.life.json.JacksonObjectMapper;
import com.life.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 通知事件 Redis Pub/Sub 监听器(管理端 -> C端 的跨服务通知通道)
 * <p>
 * 频道: life:notice:push(见 RedisKeyConstant.NOTICE_PUSH_CHANNEL)
 * 报文: {"event":"ANNOUNCEMENT_PUBLISHED","announcementId":1,"title":"...","content":"..."}
 * <p>
 * 背景: 公告在 life-admin(RuoYi) 发布, 而用户 WebSocket 会话在 life-social,
 * 代码不迁移, 事件驱动打通(一期 Redis Pub/Sub, 二期升级 RabbitMQ, 见路线图 2.12)。
 * 监听线程无 BaseContext, 收件箱公共字段由自动填充器兜底(creater为空表示系统投递)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeEventListener implements MessageListener {

    /**
     * 公告发布事件
     */
    public static final String EVENT_ANNOUNCEMENT_PUBLISHED = "ANNOUNCEMENT_PUBLISHED";

    private static final ObjectMapper EVENT_MAPPER = new JacksonObjectMapper();

    private final NoticeService noticeService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = message.getBody() == null ? "" : new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            Map<String, Object> event = EVENT_MAPPER.readValue(body, Map.class);
            String type = String.valueOf(event.get("event"));
            if (EVENT_ANNOUNCEMENT_PUBLISHED.equals(type)) {
                Long announcementId = Long.valueOf(String.valueOf(event.get("announcementId")));
                String title = String.valueOf(event.get("title"));
                String content = String.valueOf(event.get("content"));
                noticeService.fanOutAnnouncement(announcementId, title, content);
            } else {
                log.debug("忽略未知通知事件: {}", body);
            }
        } catch (Exception e) {
            // 事件消费失败不影响管理端(DB为准, C端下次拉取列表仍可见), 一期仅告警
            log.error("通知事件处理失败: {}", body, e);
        }
    }
}

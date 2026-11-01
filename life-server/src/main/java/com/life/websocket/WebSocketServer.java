package com.life.websocket;

import com.life.constant.JwtClaimsConstant;
import com.life.constant.RedisKeyConstant;
import com.life.json.JacksonObjectMapper;
import com.life.properties.JwtProperties;
import com.life.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务(站内通知实时推送通道)
 * <p>
 * 归属决策(1.3): 放在 C端服务 life-social, 不迁移到 life-admin(RuoYi)。
 * 依据: WebSocket 连接的本质是"接收方的在线会话"。通知的接收者是 C端用户,
 * 用户的登录态(JWT/登出黑名单)与绝大多数通知产生事件(支付/积分/权益)都在本服务内,
 * 只有持有用户会话的进程才能点对点推送; 管理端(RuoYi)与用户无关, 且属内网服务,
 * 让 C端用户连管理端 WebSocket 是安全反模式。
 * 管理端触发的通知(系统公告发布)不搬代码, 通过 Redis Pub/Sub 发事件, 由本服务的
 * NoticeEventListener 订阅后落收件箱并推送, 二期升级 RabbitMQ(见路线图 2.12)。
 * <p>
 * 连接方式: ws://{host}:8080/ws/{token}, token 与 HTTP 请求头 authentication 相同。
 * 握手时校验 JWT 与登出黑名单, 以 userId 为键登记会话(替代苍穹版随机 sid+广播),
 * 未认证连接直接关闭。推送报文见 NoticePushVO。
 */
@Component
@Slf4j
@ServerEndpoint("/ws/{token}")
public class WebSocketServer {

    /**
     * 在线会话: userId -> 该用户的多端会话(同一账号可多标签页/设备同时在线)
     */
    private static final Map<Long, Map<String, Session>> USER_SESSIONS = new ConcurrentHashMap<>();

    /**
     * 推送报文序列化(复用项目 Jackson 配置, LocalDateTime 按全局格式输出)
     */
    private static final ObjectMapper PUSH_MAPPER = new JacksonObjectMapper();

    // ---- @ServerEndpoint 由容器按连接实例化, Spring 依赖需经静态字段中转 ----

    private static JwtProperties jwtProperties;

    private static StringRedisTemplate stringRedisTemplate;

    @Autowired
    public void setJwtProperties(JwtProperties properties) {
        WebSocketServer.jwtProperties = properties;
    }

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate redisTemplate) {
        WebSocketServer.stringRedisTemplate = redisTemplate;
    }

    /**
     * 连接建立: 校验token(含登出黑名单), 解析userId后登记会话; 失败拒绝连接
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        Long userId = resolveUserId(token);
        if (userId == null) {
            log.warn("WebSocket握手拒绝: token无效或已登出, sessionId={}", session.getId());
            closeQuietly(session, "invalid token");
            return;
        }
        USER_SESSIONS.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
        log.info("WebSocket建立连接: userId={}, sessionId={}, 在线用户数={}", userId, session.getId(), USER_SESSIONS.size());
        sendRaw(session, "{\"event\":\"connected\"}");
    }

    /**
     * 收到客户端消息: 一期仅心跳(ping->pong), 业务指令走HTTP
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        if ("ping".equalsIgnoreCase(message)) {
            sendRaw(session, "{\"event\":\"pong\"}");
        }
    }

    /**
     * 连接关闭: 摘除会话, 用户无在线会话时移除键
     */
    @OnClose
    public void onClose(@PathParam("token") String token, Session session) {
        Long userId = resolveUserId(token);
        if (userId == null) {
            return;
        }
        USER_SESSIONS.computeIfPresent(userId, (k, sessions) -> {
            sessions.remove(session.getId());
            return sessions.isEmpty() ? null : sessions;
        });
        log.info("WebSocket断开连接: userId={}, sessionId={}, 在线用户数={}", userId, session.getId(), USER_SESSIONS.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.debug("WebSocket异常: sessionId={}, {}", session.getId(), error.getMessage());
        closeQuietly(session, "error");
    }

    /**
     * 点对点推送(序列化失败/发送失败仅记日志, 不影响收件箱落库主流程)
     */
    public void sendToUser(Long userId, Object payload) {
        Map<String, Session> sessions = USER_SESSIONS.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String json;
        try {
            json = PUSH_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("WebSocket推送序列化失败: userId={}", userId, e);
            return;
        }
        sessions.values().forEach(session -> sendRaw(session, json));
    }

    /**
     * 用户是否在线(有任一会话)
     */
    public boolean isOnline(Long userId) {
        Set<String> sessionIds = USER_SESSIONS.containsKey(userId) ? USER_SESSIONS.get(userId).keySet() : null;
        return sessionIds != null && !sessionIds.isEmpty();
    }

    /**
     * 当前在线用户ID集合(公告fan-out后仅推送在线用户, 离线用户下次拉列表可见)
     */
    public java.util.Set<Long> onlineUserIds() {
        return USER_SESSIONS.keySet();
    }

    /**
     * 群发(保留原广播能力, 供后续全站广播类需求使用)
     *
     * @param message 报文
     */
    public void sendToAllClient(String message) {
        USER_SESSIONS.values().forEach(sessions ->
                sessions.values().forEach(session -> sendRaw(session, message)));
    }

    /**
     * 向单个会话发送文本(getBasicRemote 非线程安全, 按会话加锁串行发送)
     */
    private void sendRaw(Session session, String text) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.getBasicRemote().sendText(text);
            }
        } catch (IOException e) {
            log.warn("WebSocket发送失败: sessionId={}, {}", session.getId(), e.getMessage());
        }
    }

    /**
     * token -> userId: 解析JWT并校验登出黑名单, 与 JwtUserInterceptor 同口径
     */
    private Long resolveUserId(String token) {
        if (token == null || token.isEmpty() || jwtProperties == null || stringRedisTemplate == null) {
            return null;
        }
        try {
            Boolean blacklisted = stringRedisTemplate.hasKey(RedisKeyConstant.USER_TOKEN_BLACKLIST + token);
            if (Boolean.TRUE.equals(blacklisted)) {
                return null;
            }
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            return Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
        } catch (Exception ex) {
            log.debug("WebSocket token解析失败: {}", ex.getMessage());
            return null;
        }
    }

    private void closeQuietly(Session session, String reason) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reason));
        } catch (IOException e) {
            log.debug("WebSocket关闭异常(忽略): {}", e.getMessage());
        }
    }
}

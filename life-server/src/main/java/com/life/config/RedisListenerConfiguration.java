package com.life.config;

import com.life.constant.RedisKeyConstant;
import com.life.listener.NoticeEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub 监听配置: 订阅管理端(life-admin)通知事件
 */
@Configuration
public class RedisListenerConfiguration {

    @Bean
    public RedisMessageListenerContainer noticeEventContainer(RedisConnectionFactory connectionFactory,
                                                              NoticeEventListener noticeEventListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(noticeEventListener, new ChannelTopic(RedisKeyConstant.NOTICE_PUSH_CHANNEL));
        return container;
    }
}

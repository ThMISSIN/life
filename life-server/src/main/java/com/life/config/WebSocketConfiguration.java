package com.life.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置
 * <p>
 * 内嵌 Tomcat 不会自动扫描 @ServerEndpoint, 必须显式注册 ServerEndpointExporter,
 * 否则 WebSocketServer 不会被发布(部署为war包时由容器注册, 该Bean需让位容器)。
 */
@Configuration
public class WebSocketConfiguration {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}

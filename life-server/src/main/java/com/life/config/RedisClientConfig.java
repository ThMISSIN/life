package com.life.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 客户端兼容性配置
 *
 * 本地 Redis 为 3.x 老版本（Windows 移植版），开启 requirepass 后：
 * Lettuce 6.4 默认握手先尝试 HELLO（RESP3 协商），老版本返回 unknown command，
 * 随后回退流程先发 SELECT/CLIENT 再 AUTH，被 requirepass 拦截为 NOAUTH，
 * 导致连接建立失败（所有需登录接口 500）。
 *
 * 强制 RESP2 后握手顺序恢复为 AUTH -> SELECT，与老版本 Redis 兼容。
 */
@Configuration
public class RedisClientConfig {

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceProtocolCustomizer() {
        return builder -> builder.clientOptions(
                ClientOptions.builder()
                        .protocolVersion(ProtocolVersion.RESP2)
                        .build());
    }
}

package com.life.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.redisson.config.Protocol;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
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
 *
 * 引入 redisson-spring-boot-starter 后, spring-data-redis 的连接工厂通常被
 * RedissonConnectionFactory 接管, 下方 Lettuce 定制器可能不再被调用;
 * 因此同步给 Redisson 也显式声明 RESP2(Redisson 默认虽为 RESP2, 显式配置防升级改变默认值),
 * 两条路都指向同一协议, 老版 Redis 兼容性不回退。
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

    @Bean
    public RedissonAutoConfigurationCustomizer redissonProtocolCustomizer() {
        return config -> config.setProtocol(Protocol.RESP2);
    }
}

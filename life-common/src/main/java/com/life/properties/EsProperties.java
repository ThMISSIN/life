package com.life.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 2.11 ES 全文检索参数(life.es.*): 低层 REST 直连(不绑定 elasticsearch-java 客户端版本,
 * 7.x/8.x 服务端均兼容); enabled=false 或连接异常时自动降级 SQL LIKE
 */
@Component
@ConfigurationProperties(prefix = "life.es")
@Data
public class EsProperties {

    /** 检索开关: false 时关键词搜索走 SQL LIKE(降级路径) */
    private boolean enabled = true;

    /** ES 地址(WSL 容器映射) */
    private String uri = "http://172.27.216.62:9200";

    /** 帖子索引名 */
    private String postIndex = "life_post";

    /** 单请求超时(毫秒) */
    private int timeoutMs = 2000;
}

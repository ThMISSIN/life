package com.life.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 2.10 接口层限流(life.sentinel.*): sentinel-core 单机规则(无 dashboard, 规则代码化),
 * 热点参数=userId 实现"单用户QPS"限流; 集群流控(token server)属二期(roadmap 4.4)
 */
@Component
@ConfigurationProperties(prefix = "life.sentinel")
@Data
public class SentinelProperties {

    /** 限流总开关 */
    private boolean enabled = true;

    /**
     * 资源规则: key=资源名(接口路径), value=单用户QPS阈值;
     * 未在表内的接口不限流
     */
    private Map<String, Integer> rules = new HashMap<>();
}

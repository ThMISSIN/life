package com.life.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 2.10 风控参数(life.risk.*): 滑动窗口限频规则 + 风险标记
 */
@Component
@ConfigurationProperties(prefix = "life.risk")
@Data
public class RiskProperties {

    /** 风控总开关(关闭后 collect/gate 全部直通, 便于排障) */
    private boolean enabled = true;

    /** 命中规则后的风险标记时长(分钟): 标记期内暂停点赞/积分领取等敏感动作 */
    private long flagTtlMinutes = 10;

    /**
     * 各动作滑动窗口规则: key=RiskActionConstant, value=窗口秒数+阈值;
     * 未配置的动作不限频
     */
    private Map<String, WindowRule> windows = new HashMap<>();

    @Data
    public static class WindowRule {
        /** 窗口长度(秒) */
        private int seconds = 60;
        /** 窗口内允许次数, 超过即命中 */
        private int limit = 30;
    }
}

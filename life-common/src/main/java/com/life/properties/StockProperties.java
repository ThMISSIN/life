package com.life.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 2.14 Redis+Lua 库存参数(life.stock.*)
 */
@Component
@ConfigurationProperties(prefix = "life.stock")
@Data
public class StockProperties {

    /** Redis 库存层开关: false 时全部走 DB 原子扣减(原路径) */
    private boolean enabled = true;

    /** 下单遇到未预热商品时自动预热(SETNX DB快照后重试Lua); false 则仅走DB路径 */
    private boolean preheatOnDemand = true;

    /** 库存镜像TTL(小时): 到期自动回落DB路径, 防长期漂移; 0=不过期 */
    private long cacheTtlHours = 72;

    /** 对账任务周期(分钟) */
    private long reconcileIntervalMinutes = 10;
}

package com.life.service;

/**
 * 2.14 Redis+Lua 库存镜像: Lua 原子"校验+扣减"前置于 DB 条件扣减(DB 仍是权威兜底),
 * 热点商品抢单压力被 Redis 挡掉大头; 未预热的商品自动降级纯 DB 路径
 */
public interface StockCacheService {

    /** 镜像条目类型: 实物商品 */
    String TYPE_PRODUCT = "product";
    /** 镜像条目类型: 权益 */
    String TYPE_BENEFIT = "benefit";

    /** Lua 预扣结果: 镜像不存在(走DB路径) */
    long MISS = -1;
    /** Lua 预扣结果: 库存不足(快速失败, 不打DB) */
    long SOLD_OUT = -2;

    /**
     * Lua 原子校验+扣减
     *
     * @return >=0 扣后剩余; MISS=未预热; SOLD_OUT=不足
     */
    long tryDeduct(String itemType, Long itemId, int quantity);

    /**
     * 回补镜像(DB扣减失败补偿 / 取消 / 关单 / 退款 / 作废): 镜像存在才 INCRBY, 尽力而为
     */
    void rollback(String itemType, Long itemId, int quantity);

    /**
     * 预热: SETNX DB 库存快照(不覆盖既有镜像, 防并发预热写脏); 不限量(-1)/负库存不预热
     *
     * @return 本次是否真正写入
     */
    boolean preheat(String itemType, Long itemId, int dbStock);

    /**
     * 对账(定时任务): 遍历预热注册表, Redis 镜像 ≠ DB 库存 → 以 DB 为准重置 + 告警日志;
     * DB 侧已删除/不限量/下架的条目直接移除镜像
     *
     * @return 校正条目数
     */
    int reconcile();
}

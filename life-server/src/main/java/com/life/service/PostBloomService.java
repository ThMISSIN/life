package com.life.service;

/**
 * 2.11 帖子id布隆过滤器(缓存穿透防线): id从未进过滤器 → 确定不存在, 单帖读取入口直接拒绝;
 * 已删id留存只产生低概率误判(mightContain=true 仍由DB判真假), 无"不存在误报存在"之外的风险
 */
public interface PostBloomService {

    /**
     * 可能存在吗? 过滤器不可用/未就绪时 fail-open 返回 true(退化为直接查库, 不误杀正常流量)
     */
    boolean mightContain(Long postId);

    /**
     * 发帖/转发成功后登记id(尽力而为, 失败仅告警——漏登记后果是新帖短暂被挡, 由降级路径兜底)
     */
    void add(Long postId);
}

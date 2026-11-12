package com.life.service;

/**
 * 2.10 风控服务: 采集层(滑动窗口计数+IP/设备指纹留痕) + 规则层(高频标记→敏感动作拦截)
 */
public interface RiskService {

    /**
     * 组合入口(敏感动作执行前调用): 已被标记风险 → 直接拦截; 否则记一次动作并评估滑动窗口,
     * 超阈值 → 打风险标记(暂停积分发放/点赞等) + 落 life_risk_record 留痕
     *
     * @param action RiskActionConstant
     * @throws com.life.exception.ValidationException 命中风险标记时
     */
    void checkAndCollect(String action, Long userId);

    /**
     * 仅判风险标记不计数(发放类旁路: MQ消费者/定时任务里无法采集请求上下文时用)
     */
    boolean isRisky(Long userId);
}

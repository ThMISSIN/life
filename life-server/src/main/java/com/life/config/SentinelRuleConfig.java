package com.life.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.life.properties.SentinelProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 2.10 接口层限流(Sentinel 单机规则, 无 dashboard —— 规则代码化自 yml life.sentinel.rules):
 * 每个受控资源两条规则——
 * ① 资源级 FlowRule: 接口总 QPS 熔断线(防整体打爆);
 * ② 热点参数 ParamFlowRule(paramIdx=0=userId): 单用户 QPS(防单人刷接口);
 * 埋点在 SentinelLimitInterceptor, 资源名=URL pattern; 集群流控属二期(roadmap 4.4)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SentinelRuleConfig {

    private final SentinelProperties properties;

    @PostConstruct
    public void loadRules() {
        if (!properties.isEnabled() || properties.getRules().isEmpty()) {
            log.info("Sentinel 限流未启用或未配置规则");
            return;
        }
        List<FlowRule> flowRules = new ArrayList<>();
        List<ParamFlowRule> paramRules = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : properties.getRules().entrySet()) {
            String resource = entry.getKey();
            int userQps = entry.getValue();

            // 单用户 QPS(热点参数)
            ParamFlowRule paramRule = new ParamFlowRule(resource)
                    .setParamIdx(0)
                    .setGrade(RuleConstant.FLOW_GRADE_QPS)
                    .setCount(userQps);
            paramRules.add(paramRule);

            // 资源总 QPS: 单用户阈值 × 20 作为整体熔断线(单实例口径)
            FlowRule flowRule = new FlowRule(resource)
                    .setGrade(RuleConstant.FLOW_GRADE_QPS)
                    .setCount(userQps * 20L);
            flowRules.add(flowRule);
        }
        FlowRuleManager.loadRules(flowRules);
        ParamFlowRuleManager.loadRules(paramRules);
        log.info("Sentinel 规则加载完成: {} 个资源 {}", flowRules.size(), properties.getRules());
    }
}

package com.life.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * AI 编排对话应答（对应 Python /agent/chat 返回结构）。
 * 入参 snake_case 经 @JsonAlias 兼容；对外序列化保持项目一致的 camelCase。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentAnswerVO {

    /** true 时表示意图置信度不足，answer 字段是澄清反问而非答案 */
    @JsonAlias("need_clarification")
    private boolean needClarification;

    private String answer;

    /** 意图（问答/任务/文档 树形根节点） */
    private String intent;

    @JsonAlias("sub_intent")
    private String subIntent;

    private Double confidence;

    /** 实际使用的编排模式：react | plan_execute */
    @JsonAlias("mode_used")
    private String modeUsed;

    private boolean success;

    /** plan_execute 失败自动降级 react 时为 true */
    private boolean degraded;

    /** Python 侧链路追踪 ID，排障用 */
    @JsonAlias("trace_id")
    private String traceId;

    private String error;

    /** 执行轨迹（紧凑版：phase/action/observation 摘要） */
    private List<StepVO> steps;

    /** 反思审查报告（可能为 null：未启用或降级） */
    private ReflectionVO reflection;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StepVO {
        private String phase;
        private String action;
        private String observation;
        private String status;
        private String error;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReflectionVO {
        @JsonAlias("quality_score")
        private Integer qualityScore;

        @JsonAlias("is_complete")
        private boolean complete;

        @JsonAlias("likely_hallucination")
        private boolean likelyHallucination;

        private List<String> suggestions;

        private String summary;
    }
}

package com.life.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 知识库检索结果（对应 Python /agent/knowledge/search 返回）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentSearchVO {

    private String query;

    private List<Item> results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String id;
        private String snippet;
        /** 完整命中原文（供需要展开时使用） */
        private String content;
        private Double score;
        /** 命中来源：vector | keyword | hybrid */
        private String source;
    }
}

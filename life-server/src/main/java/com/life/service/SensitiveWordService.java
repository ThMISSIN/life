package com.life.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 2.9 敏感词过滤(DFA): 词库 life_sensitive_word, 内存字典树 + 定时刷新
 */
public interface SensitiveWordService {

    /**
     * 过滤文本: 返回命中详情与替换结果
     */
    FilterResult filter(String text);

    /**
     * 重建内存词典(词库增删改后调用; 另有定时兜底刷新)
     */
    void refresh();

    /**
     * 过滤结果: maxLevel 取命中词最严策略(1-拒绝 2-审核 3-替换), 0=未命中
     */
    @Data
    @AllArgsConstructor
    class FilterResult {
        private boolean hit;
        private int maxLevel;
        /** 命中词列表(留痕/日志用) */
        private List<String> words;
        /** level=3 替换后的文本(命中词→等长*), 未命中或未替换时=原文 */
        private String replaced;

        public static FilterResult clean(String text) {
            return new FilterResult(false, 0, List.of(), text);
        }
    }
}

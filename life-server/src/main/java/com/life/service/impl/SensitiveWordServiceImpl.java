package com.life.service.impl;

import com.life.entity.LifeSensitiveWord;
import com.life.mapper.LifeSensitiveWordMapper;
import com.life.service.SensitiveWordService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2.9 DFA 敏感词过滤实现
 * <p>词典: Map 嵌套前缀树, 叶子挂 level; 匹配 O(文本长×词长) 与词库规模无关;
 * 归一化: 统一小写; 容忍词间插入分隔符(空格/常见符号, 最多容忍2个防误伤);
 * 替换策略: level3 命中词整体替换为等长 *; volatile 整树原子替换, 刷新期间读不受影响
 */
@Service
@Slf4j
public class SensitiveWordServiceImpl implements SensitiveWordService {

    /** 叶子节点上挂载词级别的 key(不可见字符, 不会与词面冲突) */
    private static final Character LEVEL_KEY = '\u0000';
    /** 匹配时容忍跳过的分隔符 */
    private static final String SKIP_CHARS = " \t\n\r.,;:!?。，；：！？、-_*#~@/\\|";

    @Autowired
    private LifeSensitiveWordMapper sensitiveWordMapper;

    private volatile Map<Character, Object> root = new HashMap<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    @Override
    public void refresh() {
        try {
            List<LifeSensitiveWord> words = sensitiveWordMapper.selectEnabled();
            Map<Character, Object> newRoot = new HashMap<>();
            for (LifeSensitiveWord w : words) {
                if (w.getWord() == null || w.getWord().isBlank()) {
                    continue;
                }
                addWord(newRoot, w.getWord().trim().toLowerCase(),
                        w.getLevel() == null ? 3 : w.getLevel());
            }
            this.root = newRoot;
            log.info("敏感词词典刷新完成: {} 词", words.size());
        } catch (Exception e) {
            // DB不可用时保留旧词典(首启失败则空词典=不过滤, 内容风险由人工审核兜底)
            log.warn("敏感词词典刷新失败, 沿用旧词典", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void addWord(Map<Character, Object> root, String word, int level) {
        Map<Character, Object> node = root;
        for (char c : word.toCharArray()) {
            Object next = node.get(c);
            if (!(next instanceof Map)) {
                next = new HashMap<Character, Object>();
                node.put(c, next);
            }
            node = (Map<Character, Object>) next;
        }
        // 同词多级别取最严(数字最小); 短词是长词前缀时叶子level共存不冲突
        Object exist = node.get(LEVEL_KEY);
        node.put(LEVEL_KEY, exist == null ? level : Math.min(level, (Integer) exist));
    }

    @Override
    @SuppressWarnings("unchecked")
    public FilterResult filter(String text) {
        if (text == null || text.isEmpty() || root.isEmpty()) {
            return FilterResult.clean(text);
        }
        String lower = text.toLowerCase();
        List<int[]> hits = new ArrayList<>();   // [start, endExclusive, level]
        List<String> hitWords = new ArrayList<>();
        int minLevel = 0;

        for (int i = 0; i < lower.length(); i++) {
            Map<Character, Object> node = root;
            int lastMatchEnd = -1;
            int lastMatchLevel = 0;
            int skipped = 0;
            for (int j = i; j < lower.length(); j++) {
                char c = lower.charAt(j);
                Object next = node.get(c);
                if (next instanceof Map) {
                    node = (Map<Character, Object>) next;
                    Object lv = node.get(LEVEL_KEY);
                    if (lv != null) {
                        // 贪婪取最长命中
                        lastMatchEnd = j + 1;
                        lastMatchLevel = (Integer) lv;
                    }
                    skipped = 0;
                } else if (SKIP_CHARS.indexOf(c) >= 0 && skipped < 2) {
                    // 容忍分隔符插入(如 "赌 博"), 最多连跳2个
                    skipped++;
                } else {
                    break;
                }
            }
            if (lastMatchEnd > 0) {
                hits.add(new int[]{i, lastMatchEnd, lastMatchLevel});
                hitWords.add(text.substring(i, lastMatchEnd));
                minLevel = minLevel == 0 ? lastMatchLevel : Math.min(minLevel, lastMatchLevel);
                i = lastMatchEnd - 1; // 跳过已命中段
            }
        }

        if (hits.isEmpty()) {
            return FilterResult.clean(text);
        }
        // level3 替换: 命中段整体打码(等长*), 保留原文其余部分
        char[] chars = text.toCharArray();
        for (int[] hit : hits) {
            if (hit[2] == 3) {
                for (int k = hit[0]; k < hit[1]; k++) {
                    chars[k] = '*';
                }
            }
        }
        return new FilterResult(true, minLevel, hitWords, new String(chars));
    }
}

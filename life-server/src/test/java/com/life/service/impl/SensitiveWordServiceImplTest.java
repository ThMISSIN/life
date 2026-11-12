package com.life.service.impl;

import com.life.entity.LifeSensitiveWord;
import com.life.mapper.LifeSensitiveWordMapper;
import com.life.service.SensitiveWordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 2.9 敏感词 DFA 过滤单测(纯内存逻辑, 不依赖 Spring 上下文/DB)
 */
class SensitiveWordServiceImplTest {

    private SensitiveWordServiceImpl service;

    private static LifeSensitiveWord word(String w, int level) {
        return LifeSensitiveWord.builder().word(w).level(level).status(1).build();
    }

    @BeforeEach
    void setUp() {
        LifeSensitiveWordMapper mapper = mock(LifeSensitiveWordMapper.class);
        when(mapper.selectEnabled()).thenReturn(List.of(
                word("赌博网站", 1),
                word("代开发票", 2),
                word("白痴", 3),
                word("spam", 3)));
        service = new SensitiveWordServiceImpl();
        ReflectionTestUtils.setField(service, "sensitiveWordMapper", mapper);
        service.refresh();
    }

    @Test
    void cleanTextNotHit() {
        SensitiveWordService.FilterResult r = service.filter("今天天气不错, 分享一个权益攻略");
        assertFalse(r.isHit());
        assertEquals(0, r.getMaxLevel());
        assertEquals("今天天气不错, 分享一个权益攻略", r.getReplaced());
    }

    @Test
    void level1RejectWordDetected() {
        SensitiveWordService.FilterResult r = service.filter("快来赌博网站充值");
        assertTrue(r.isHit());
        assertEquals(1, r.getMaxLevel());
        assertTrue(r.getWords().contains("赌博网站"));
    }

    @Test
    void level2ReviewWordDetected() {
        SensitiveWordService.FilterResult r = service.filter("专业代开发票联系我");
        assertTrue(r.isHit());
        assertEquals(2, r.getMaxLevel());
    }

    @Test
    void level3WordMaskedWithSameLength() {
        SensitiveWordService.FilterResult r = service.filter("你这个白痴啊");
        assertTrue(r.isHit());
        assertEquals(3, r.getMaxLevel());
        assertEquals("你这个**啊", r.getReplaced());
    }

    @Test
    void caseInsensitiveAndSeparatorTolerant() {
        // 大小写归一
        assertTrue(service.filter("BUY SPAM NOW").isHit());
        // 分隔符插入绕过("赌 博 网 站" 词内跳格最多容忍2个分隔符, 连续4段应至少命中前缀词或整体,
        // 此处断言"赌 博网站"(1个分隔符)可命中)
        SensitiveWordService.FilterResult r = service.filter("赌 博网站走起");
        assertTrue(r.isHit());
        assertEquals(1, r.getMaxLevel());
    }

    @Test
    void mostSevereLevelWins() {
        // 同时命中 level3 与 level1 → 取最严(1)
        SensitiveWordService.FilterResult r = service.filter("白痴才去赌博网站");
        assertTrue(r.isHit());
        assertEquals(1, r.getMaxLevel());
    }

    @Test
    void nullAndEmptySafe() {
        assertFalse(service.filter(null).isHit());
        assertFalse(service.filter("").isHit());
    }
}

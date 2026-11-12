package com.life.service.impl;

import com.life.constant.PointsRecordStatusConstant;
import com.life.constant.PointsTypeConstant;
import com.life.entity.LifePointsRecord;
import com.life.exception.ValidationException;
import com.life.mapper.LifePointsRecordMapper;
import com.life.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 2.2 changePoints 统一积分入口单测: 幂等拦截 / 正负向入账 / 余额不足 / 冻结口径
 */
@ExtendWith(MockitoExtension.class)
class PointsServiceImplChangePointsTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private LifePointsRecordMapper pointsRecordMapper;

    @InjectMocks
    private PointsServiceImpl pointsService;

    @BeforeEach
    void setUp() {
        // expire-days 默认0(永久), 发放不写 expire_time
        ReflectionTestUtils.setField(pointsService, "expireDays", 0);
        // 消费返积分默认配置: 1分/元 + 阶梯 50:20,100:50,300:200(达档累加)
        ReflectionTestUtils.setField(pointsService, "rebateEnabled", true);
        ReflectionTestUtils.setField(pointsService, "rebatePerYuan", 1);
        ReflectionTestUtils.setField(pointsService, "rebateTiers", "50:20,100:50,300:200");
    }

    @Test
    void consumeRebateBasePlusCumulativeTiers() {
        // 不足1元与0元不返
        assertEquals(0, pointsService.computeConsumeRebate(0));
        assertEquals(0, pointsService.computeConsumeRebate(99));
        // 49元: 仅基础返
        assertEquals(49, pointsService.computeConsumeRebate(4999));
        // 50元: 基础50 + 第一档20
        assertEquals(70, pointsService.computeConsumeRebate(5000));
        // 100元: 100 + 20 + 50
        assertEquals(170, pointsService.computeConsumeRebate(10000));
        // 300元: 300 + 20 + 50 + 200(力度越大加赠越多)
        assertEquals(570, pointsService.computeConsumeRebate(30000));
        // 分位向下取整: 89.99元按89元计 + 20
        assertEquals(109, pointsService.computeConsumeRebate(8999));
    }

    @Test
    void consumeRebateDisabledOrBadTierConfig() {
        ReflectionTestUtils.setField(pointsService, "rebateEnabled", false);
        assertEquals(0, pointsService.computeConsumeRebate(30000));
        ReflectionTestUtils.setField(pointsService, "rebateEnabled", true);
        // 坏档配置只跳过该档, 不影响其余计算
        ReflectionTestUtils.setField(pointsService, "rebateTiers", "50:xx,100:50");
        assertEquals(150, pointsService.computeConsumeRebate(10000));
    }

    @Test
    void duplicateBizNoSkippedWithoutSideEffect() {
        when(pointsRecordMapper.selectCount(any())).thenReturn(1L);

        boolean applied = pointsService.changePoints(1L, 10, PointsTypeConstant.SIGN,
                "SIGN:1:20261112", "签到奖励");

        assertFalse(applied);
        verify(userMapper, never()).addPoints(anyLong(), anyInt());
        verify(pointsRecordMapper, never()).insert(any(LifePointsRecord.class));
    }

    @Test
    void positiveChangeAddsBalanceAndSettledRecord() {
        when(pointsRecordMapper.selectCount(any())).thenReturn(0L);

        boolean applied = pointsService.changePoints(1L, 10, PointsTypeConstant.SIGN,
                "SIGN:1:20261112", "签到奖励");

        assertTrue(applied);
        verify(userMapper).addPoints(1L, 10);
        ArgumentCaptor<LifePointsRecord> captor = ArgumentCaptor.forClass(LifePointsRecord.class);
        verify(pointsRecordMapper).insert(captor.capture());
        LifePointsRecord record = captor.getValue();
        assertEquals(10, record.getPoints());
        assertEquals(PointsRecordStatusConstant.SETTLED, record.getStatus());
        assertEquals("SIGN:1:20261112", record.getBizNo());
        assertNull(record.getExpireTime());
    }

    @Test
    void expireDaysConfiguredWritesExpireTimeOnGrant() {
        ReflectionTestUtils.setField(pointsService, "expireDays", 365);
        when(pointsRecordMapper.selectCount(any())).thenReturn(0L);

        pointsService.changePoints(1L, 100, PointsTypeConstant.ACTIVITY, "TASKCLAIM:9", null, "任务奖励");

        ArgumentCaptor<LifePointsRecord> captor = ArgumentCaptor.forClass(LifePointsRecord.class);
        verify(pointsRecordMapper).insert(captor.capture());
        assertTrue(captor.getValue().getExpireTime() != null);
    }

    @Test
    void negativeChangeInsufficientBalanceThrows() {
        when(pointsRecordMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.deductPoints(1L, 5000)).thenReturn(0);

        assertThrows(ValidationException.class, () ->
                pointsService.changePoints(1L, -5000, PointsTypeConstant.EXCHANGE, "EX:LP1", "LP1", null));
        verify(pointsRecordMapper, never()).insert(any(LifePointsRecord.class));
    }

    @Test
    void freezeInsufficientBalanceThrows() {
        when(pointsRecordMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.freezePoints(1L, 5000)).thenReturn(0);

        assertThrows(ValidationException.class, () ->
                pointsService.freezePoints(1L, 5000, "PF:LP1", "LP1", "抵扣冻结"));
        verify(pointsRecordMapper, never()).insert(any(LifePointsRecord.class));
    }

    @Test
    void freezeWritesNegativeFrozenRecord() {
        when(pointsRecordMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.freezePoints(1L, 300)).thenReturn(1);

        boolean applied = pointsService.freezePoints(1L, 300, "PF:LP2", "LP2", "抵扣冻结");

        assertTrue(applied);
        ArgumentCaptor<LifePointsRecord> captor = ArgumentCaptor.forClass(LifePointsRecord.class);
        verify(pointsRecordMapper).insert(captor.capture());
        assertEquals(-300, captor.getValue().getPoints());
        assertEquals(PointsRecordStatusConstant.FROZEN, captor.getValue().getStatus());
    }

    @Test
    void zeroPointsNoop() {
        assertFalse(pointsService.changePoints(1L, 0, PointsTypeConstant.SIGN, "X", null));
        verify(pointsRecordMapper, never()).selectCount(any());
    }
}

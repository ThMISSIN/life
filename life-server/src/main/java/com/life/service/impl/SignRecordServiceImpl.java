package com.life.service.impl;

import com.life.constant.RedisKeyConstant;
import com.life.constant.SignRewardConstant;
import com.life.context.BaseContext;
import com.life.entity.BaseEntity;
import com.life.entity.SignResultVO;
import com.life.exception.BaseException;
import com.life.service.LifePointsRecordService;
import com.life.service.SignRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SignRecordServiceImpl implements SignRecordService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private LifePointsRecordService lifePointsRecordService;

    @Override
    public SignResultVO addSignRecords() {
        // 获取登录用户
        Long userId = BaseContext.getCurrentId();
        // 获取当前日期
        LocalDate now = LocalDate.now();
        // key
        String key = RedisKeyConstant.SIGN_RECORD + userId
                + now.format(DateTimeFormatter.ofPattern(RedisKeyConstant.SIGN_MONTH_FORMAT));
        // 计算offset
        int offset = now.getDayOfMonth() - 1;
        // 使用redis bitmap 记录签到信息
        Boolean res = stringRedisTemplate.opsForValue().setBit(key, offset, true);
        if (Boolean.TRUE.equals(res)) {
            throw new BaseException("不允许重复签到");
        }

        // 计算连续签到天数
        int continueDays = countSignDays(key, now.getDayOfMonth());

        // 奖励分数
        int rewardPoints = 0;
        switch (continueDays) {
            case SignRewardConstant.CONTINUE_DAYS_7:
                rewardPoints = SignRewardConstant.REWARD_POINTS_7;
                break;
            case SignRewardConstant.CONTINUE_DAYS_14:
                rewardPoints = SignRewardConstant.REWARD_POINTS_14;
                break;
            case SignRewardConstant.CONTINUE_DAYS_28:
                rewardPoints = SignRewardConstant.REWARD_POINTS_28;
                break;
        }

        // TODO 后续升级为 rabbitMq
        lifePointsRecordService.addSignRecord(userId, rewardPoints + SignRewardConstant.BASE_POINTS);

        SignResultVO vo = new SignResultVO();
        vo.setSignDays(continueDays);
        vo.setRewardPoints(rewardPoints);


        return vo;
    }

    @Override
    public Byte[] querySignRecords() {
        // 获取当前时间
        LocalDate now = LocalDate.now();
        // 获取登录用户
        Long userId = BaseContext.getCurrentId();
        String key = RedisKeyConstant.SIGN_RECORD + userId
                + now.format(DateTimeFormatter.ofPattern(RedisKeyConstant.SIGN_MONTH_FORMAT));

        int dayOfMonth = now.getDayOfMonth();

        // 查询签到记录
        List<Long> result = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands
                        .create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (CollectionUtils.isEmpty(result)) {
            return new Byte[0];
        }
        int num = result.get(0).intValue();

        Byte[] bytes = new Byte[dayOfMonth];
        int pos = dayOfMonth - 1;

        while (pos >= 0) {
            bytes[pos--] = (byte) (num & 1);
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }

        return bytes;
    }

    private int countSignDays(String key, int dayOfMonth) {
        // 获取从本月第一天起到今天的所有签到记录
        List<Long> bitMapRes = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands
                        .create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (bitMapRes == null || bitMapRes.isEmpty()) {
            return 0;
        }

        int continueSignDays = 0;
        int res = bitMapRes.getFirst().intValue();
        // 循环，与1做与运算，得到最后一个bit，判断是否为0，为0则终止，为1则继续
        while ((res & 1) == 1) {
            continueSignDays++;
            // 把数字右移一位，最后一位被舍弃，倒数第二位成了最后一位
            // 从后往前数，统计签到的天数，直到遇到0为止。所以需要右移一位
            res >>>= 1;
        }


        return continueSignDays;
    }
}

package com.life.service.impl;

import com.life.constant.MqConstant;
import com.life.constant.RedisConstant;
import com.life.constant.RiskActionConstant;
import com.life.constant.SignRewardConstant;
import com.life.constant.TaskEventTypeConstant;
import com.life.context.BaseContext;
import com.life.dto.MqPointsMessage;
import com.life.entity.SignResultVO;
import com.life.exception.BaseException;
import com.life.mapper.LifeSignRecordMapper;
import com.life.mq.MqProducer;
import com.life.service.LifePointsRecordService;
import com.life.service.RiskService;
import com.life.service.SignRecordService;
import com.life.service.TaskCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class SignRecordServiceImpl implements SignRecordService {

    /** 幂等键日期段格式(与消费端/流水 bizNo 口径一致) */
    private static final DateTimeFormatter BIZ_DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    /**
     * 签到分值(2026-11-12 提高并配置化, yml life.sign.*): 基础分与连签里程碑奖励;
     * 连签天数档位仍用 SignRewardConstant(7/14/28), 分值运营可调无需发版
     */
    @Value("${life.sign.base-points:5}")
    private int basePoints;
    @Value("${life.sign.reward-7:30}")
    private int reward7;
    @Value("${life.sign.reward-14:60}")
    private int reward14;
    @Value("${life.sign.reward-28:150}")
    private int reward28;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private MqProducer mqProducer;
    @Autowired
    private LifeSignRecordMapper signRecordMapper;
    @Autowired
    private LifePointsRecordService pointsRecordService;
    @Autowired
    private RiskService riskService;
    @Autowired
    private TaskCenterService taskCenterService;

    @Override
    public SignResultVO addSignRecords() {
        // 获取登录用户
        Long userId = BaseContext.getCurrentId();
        // 2.10 风控闸门: 被标记风险的用户暂停签到发放(滑动窗口对签到意义不大, 主要拦机器号)
        riskService.checkAndCollect(RiskActionConstant.SIGN, userId);
        // 获取当前日期
        LocalDate now = LocalDate.now();
        // key
        String key = RedisConstant.SIGN_RECORD + userId
                + now.format(DateTimeFormatter.ofPattern(RedisConstant.SIGN_MONTH_FORMAT));
        // 计算offset
        int offset = now.getDayOfMonth() - 1;
        // 使用redis bitmap 记录签到信息
        Boolean res = stringRedisTemplate.opsForValue().setBit(key, offset, true);
        if (Boolean.TRUE.equals(res)) {
            throw new BaseException("不允许重复签到");
        }
        // 3.5: bitmap key 设 62 天 TTL(跨月自然过期, 修复"key永不过期、Redis清空即全丢"隐患;
        // 持久化兜底见下方 life_sign_record 落库)
        stringRedisTemplate.expire(key, Duration.ofDays(62));

        // 计算连续签到天数
        int continueDays = countSignDays(key, now.getDayOfMonth());

        // 奖励分数(配置化, 2026-11-12 提高: 基础5 / 连签7天+30 / 14天+60 / 28天+150)
        int rewardPoints = 0;
        switch (continueDays) {
            case SignRewardConstant.CONTINUE_DAYS_7:
                rewardPoints = reward7;
                break;
            case SignRewardConstant.CONTINUE_DAYS_14:
                rewardPoints = reward14;
                break;
            case SignRewardConstant.CONTINUE_DAYS_28:
                rewardPoints = reward28;
                break;
        }
        int totalPoints = rewardPoints + basePoints;

        // 3.5 签到落库: INSERT IGNORE + uk_user_date 幂等, Redis 数据丢失后可按本表补账
        try {
            signRecordMapper.insertIgnore(userId, now, continueDays, totalPoints);
        } catch (Exception e) {
            log.warn("签到落库失败(不阻断签到, bitmap/流水仍在) userId={}", userId, e);
        }

        // 2.12 签到奖励走 RabbitMQ 异步发放: points.direct/sign.in → sign.point.queue → AddPointsListener
        // → changePoints(bizNo=SIGN:{userId}:{yyyyMMdd} 唯一索引幂等, at-least-once 重投不重复入账);
        // broker 不可用时降级同步入账(同一 bizNo, 与MQ路径互斥不双发)
        String bizNo = "SIGN:" + userId + ":" + now.format(BIZ_DATE_FMT);
        MqPointsMessage msg = MqPointsMessage.builder()
                .userId(userId).points(totalPoints).bizNo(bizNo).remark("签到奖励").build();
        try {
            mqProducer.send(MqConstant.POINTS_EXCHANGE, MqConstant.POINTS_SIGN_ROUTING_KEY, msg);
        } catch (Exception e) {
            log.warn("签到MQ不可用, 降级同步入账 bizNo={}", bizNo, e);
            pointsRecordService.addSignRecord(userId, totalPoints, bizNo);
        }

        // 2.4 任务事件: 每日签到任务落完成记录(待用户领取)
        taskCenterService.triggerEvent(userId, TaskEventTypeConstant.SIGN);

        SignResultVO vo = new SignResultVO();
        vo.setSignDays(continueDays);
        vo.setSignPoints(basePoints);
        vo.setRewardPoints(rewardPoints);
        return vo;
    }

    @Override
    public Byte[] querySignRecords() {
        // 获取当前时间
        LocalDate now = LocalDate.now();
        // 获取登录用户
        Long userId = BaseContext.getCurrentId();
        String key = RedisConstant.SIGN_RECORD + userId
                + now.format(DateTimeFormatter.ofPattern(RedisConstant.SIGN_MONTH_FORMAT));

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
            // 把数字右移一位，抛弃最后一个bit位
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
        // 循环，与1做与运算，判断最后一个bit位是否为0，为0则终止，为1则继续
        while ((res & 1) == 1) {
            continueSignDays++;
            // 把数字右移一位，最后一位被舍弃，倒数第二位成了最后一位
            // 从后往前数，统计签到的天数，直到遇到0为止。所以需要右移一位
            res >>>= 1;
        }

        return continueSignDays;
    }
}

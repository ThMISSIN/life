package com.life.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.BizTypeConstant;
import com.life.constant.MqConstant;
import com.life.constant.RedisConstant;
import com.life.context.BaseContext;
import com.life.dto.LikeRecordDTO;
import com.life.dto.LikedTimesDTO;
import com.life.dto.MqLikeBatchMessage;
import com.life.entity.LifeLikedRecord;
import com.life.exception.ValidationException;
import com.life.mapper.LifeLikedRecordMapper;
import com.life.service.LikeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class LikeServiceImpl extends ServiceImpl<LifeLikedRecordMapper, LifeLikedRecord> implements LikeService {
    
    /**
     * 已支持的业务类型白名单: 回写目标表必须真实存在且有like_count列;
     * BENEFIT永久排除(life_benefit无like_count列且永不添加); GOODS/ACTIVITY业务模块建表后,
     * 在此追加 + 补Mapper回写方法 + 任务轮询列表加类型, 三处一起动
     */
    private static final Set<String> SUPPORTED_BIZ_TYPES =
            Set.of(BizTypeConstant.POST, BizTypeConstant.COMMENT);

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private com.life.mq.MqProducer mqProducer;
    @Autowired
    private com.life.service.RiskService riskService;

    @Override
    public void addLikeRecord(LikeRecordDTO recordDto) {
        requireSupportedBizType(recordDto.getBizType());
        // 2.10 风控闸门: 被标记风险的用户暂停点赞(采集+滑动窗口评估在 checkAndCollect 内)
        riskService.checkAndCollect(com.life.constant.RiskActionConstant.LIKE, BaseContext.getCurrentId());
        // 判断是点赞还是取消点赞
        Boolean success = recordDto.getLiked() ? like(recordDto) : unlike(recordDto);
        // 失败时直接返回
        if (!success) {
            return;
        }
        // 点赞成功 统计点赞数
        Long likeCount = redisTemplate.opsForSet().size(bizSetKey(recordDto.getBizType(), recordDto.getBizId()));
        if (likeCount == null) {
            return;
        }
        //4.使用zset将点赞数缓存到redis中
        redisTemplate.opsForZSet().add(
                RedisConstant.LIKES_TIMES.replace("{bizType}", recordDto.getBizType()),
                recordDto.getBizId().toString(),
                likeCount
        );
    }

    @Override
    public Set<Long> isBizLiked(String bizType, List<Long> bizIds) {
        requireSupportedBizType(bizType);
        //1.获取登录用户
        Long userId = BaseContext.getCurrentId();
        //2.查询该用户是否对某个业务点赞
        //使用管道模式 将所有redis命令打包 发送给redis 进行批量执行 减少网络传输
        //最后返回的结果 将会一一对应 即 bizIds的索引 对应的点赞结果 objects的索引
        List<Object> objects = redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                StringRedisConnection src = (StringRedisConnection) connection;
                for (Long bizId : bizIds) {
                    src.sIsMember(bizSetKey(bizType, bizId), userId.toString());
                }
                return null;
            }
        });
        /*Set<Long> collect = new HashSet<>();
        for (int i = 0; i < objects.size(); i++) {
            Boolean o = (Boolean) objects.get(i);
            if (o) {
                collect.add(bizIds.get(i));
            }
        }*/
        return IntStream.range(0, objects.size())
                .filter(i -> (boolean) objects.get(i))
                .mapToObj(bizIds::get)
                .collect(Collectors.toSet());
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        //1.读取redis中的点赞数(暂不移除, 投递成功后再删, 防MQ故障丢计数)
        String key = RedisConstant.LIKES_TIMES.replace("{bizType}", bizType);
        // 从小到大取 maxBizSize 个: 点赞数多的对数值不敏感, 少的机会少需优先回写。
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().rangeWithScores(key, 0, maxBizSize - 1L);
        // zset不存在/本轮无变更时直接退出, 防空转
        if (tuples == null || tuples.isEmpty()) {
            return;
        }
        //2. 数据转换
        List<LikedTimesDTO> list = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String bizId = tuple.getValue();
            Double likeTimes = tuple.getScore();
            list.add(LikedTimesDTO.of(Long.valueOf(bizId), likeTimes.intValue()));
        }
        //3. 2.12 已切MQ: 整批投递 like.record.exchange → LikeRecordListener 消费落库(like_count整写覆盖幂等
        //   + 帖子顺路刷热度); 投递成功才 removeRange —— 失败保留zset下轮重投(at-least-once, 终值覆盖不漂移)
        boolean sent = mqProducer.sendQuiet(MqConstant.LIKE_RECORD_EXCHANGE, MqConstant.LIKE_RECORD_ROUTING_KEY,
                MqLikeBatchMessage.builder().bizType(bizType).items(list).build());
        if (!sent) {
            log.warn("点赞回写MQ投递失败, 保留zset下轮重试 bizType={} size={}", bizType, list.size());
            return;
        }
        redisTemplate.opsForZSet().removeRange(key, 0, maxBizSize - 1L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLikeRecord(String bizType, int maxBizSize) {
        // 1. 按业务类型扫描: like:set:biz:{bizType}:* (类型入key, 只捞本类型的set, 防止串类型)
        String typePrefix = RedisConstant.LIKES_BIZ_PREFIX + bizType + ":";
        String pattern = typePrefix + "*";

        // 2. 使用Redis的SCAN命令扫描匹配的key
        Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keySet = new HashSet<>();
            Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build());
            while (cursor.hasNext()) {
                keySet.add(new String(cursor.next()));
                if (keySet.size() >= maxBizSize) {
                    break;
                }
            }
            return keySet;
        });

        if (keys.isEmpty()) {
            return;
        }
        //批量处理点赞记录
        List<LifeLikedRecord> list = new ArrayList<>(keys.size());
        for (String key : keys) {
            String bizIdStr = key.substring(typePrefix.length());
            Long bizId = Long.valueOf(bizIdStr);

            //查询点赞用户id集合
            Set<String> userIds = redisTemplate.opsForSet().members(key);
            if (userIds == null || userIds.isEmpty()) {
                continue;
            }

            //批量插入数据库
            for (String userIdStr : userIds) {
                Long userId = Long.valueOf(userIdStr);
                LifeLikedRecord likedRecord = new LifeLikedRecord();
                likedRecord.setUserId(userId);
                likedRecord.setBizId(bizId);
                likedRecord.setBizType(bizType);
                likedRecord.setCreateTime(LocalDateTime.now());
                likedRecord.setUpdateTime(LocalDateTime.now());
                list.add(likedRecord);
            }
        }
        if (!list.isEmpty()){
            // 先物理删除这批biz的旧记录再整体重建: 本表由Redis快照全量覆盖, 历史行无留存价值;
            // 若走MP逻辑删除(UPDATE deleted=1), 尸体行仍占uk_user_biz, saveBatch插回必撞键
            List<Long> bizIds = list.stream().map(LifeLikedRecord::getBizId).distinct().collect(Collectors.toList());
            baseMapper.deleteByBiz(bizType, bizIds);

            // 批量插入新记录
            this.saveBatch(list);
        }

    }

    private Boolean unlike(LikeRecordDTO recordDto) {
        // 获取用户id
        Long userId = BaseContext.getCurrentId();
        // 获取key
        String key = bizSetKey(recordDto.getBizType(), recordDto.getBizId());
        // 执行 SREM 命令
        Long res = redisTemplate.opsForSet().remove(key, userId.toString());
        return res != null && res > 0;
    }

    private Boolean like(LikeRecordDTO recordDto) {
        // 获取用户id
        Long userId = BaseContext.getCurrentId();
        // 获取key
        String key = bizSetKey(recordDto.getBizType(), recordDto.getBizId());
        // 执行 SADD 命令
        Long res = redisTemplate.opsForSet().add(key, userId.toString());
        return res != null && res > 0;
    }

    /**
     * 拼点赞用户集合key: like:set:biz:{bizType}:{bizId}
     */
    private String bizSetKey(String bizType, Long bizId) {
        return RedisConstant.LIKES_BIZ
                .replace("{bizType}", bizType)
                .replace("{bizId}", bizId.toString());
    }

    /**
     * 业务类型白名单校验: 挡住GOODS/ACTIVITY等回写目标未建成的类型,
     * 防止Redis攒出无人消费、也无处落库的孤儿计数
     */
    private void requireSupportedBizType(String bizType) {
        if (!SUPPORTED_BIZ_TYPES.contains(bizType)) {
            throw new ValidationException("不支持的业务类型: " + bizType);
        }
    }
}

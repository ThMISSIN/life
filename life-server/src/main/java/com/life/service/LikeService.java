package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.dto.LikeRecordDTO;
import com.life.entity.LifeLikedRecord;

import java.util.List;
import java.util.Set;

public interface LikeService extends IService<LifeLikedRecord> {

    void addLikeRecord(LikeRecordDTO recordDto);

    Set<Long> isBizLiked(String bizType, List<Long> bizIds);

    /**
     * 消化点赞计数zset: popMin取出并删除→按bizType同步回写业务表like_count;
     * 一期直写, 升级MQ后此处改为投递消息
     */
    void readLikedTimesAndSendMessage(String bizType, int maxBizSize);

    /**
     * 落库点赞明细: 按bizType扫描Redis点赞SET, 物理删除旧行后整批重建
     */
    void saveLikeRecord(String bizType, int maxBizSize);
}

package com.life.task;

import com.life.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.life.constant.BizTypeConstant.COMMENT;
import static com.life.constant.BizTypeConstant.POST;

/**
 * 点赞链路定时任务: 每20s排空计数ZSet投递MQ(2.12 后落库在 LikeRecordListener 消费端)
 * + 点赞明细快照落库; 4.7 起经 TaskLockSupport 分布式锁, 多实例部署只有一台执行
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LikedTimesCheckTask {

    private static final List<String> BIZ_TYPES = List.of(POST, COMMENT);
    private static final int MAX_BIZ_SIZE = 30;

    private final LikeService likeService;
    private final TaskLockSupport taskLock;

    @Scheduled(fixedDelay = 20000)
    public void checkLikedTimes() {
        taskLock.runWithLock("likedTimesCheck", () -> {
            for (String bizType : BIZ_TYPES) {
                try {
                    likeService.readLikedTimesAndSendMessage(bizType, MAX_BIZ_SIZE);
                    likeService.saveLikeRecord(bizType, MAX_BIZ_SIZE);
                } catch (Exception e) {
                    log.warn("点赞回写轮次异常 bizType={} (下轮自愈)", bizType, e);
                }
            }
        });
    }

}

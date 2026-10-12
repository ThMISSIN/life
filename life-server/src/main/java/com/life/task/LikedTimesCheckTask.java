package com.life.task;

import com.life.constant.BizTypeConstant;
import com.life.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.life.constant.BizTypeConstant.*;

@Component
@RequiredArgsConstructor
public class LikedTimesCheckTask {

    private static final List<String> BIZ_TYPES = List.of(POST, COMMENT);
    private static final int MAX_BIZ_SIZE = 30;

    private final LikeService likeService;

    @Scheduled(fixedDelay = 20000)
    private void checkLikedTimes() {
        for (String bizType : BIZ_TYPES) {
            likeService.readLikedTimesAndSendMessage(bizType, MAX_BIZ_SIZE);
            likeService.saveLikeRecord(bizType, MAX_BIZ_SIZE);
        }
    }

}

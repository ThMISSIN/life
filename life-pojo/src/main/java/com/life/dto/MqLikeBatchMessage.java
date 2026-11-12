package com.life.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * MQ点赞回写报文(2.12): 回写任务排空 ZSet 后整批投递, 消费端落 like_count + 刷新热度;
 * score 为点赞SET size 终值, 整写覆盖天然幂等, 重投不漂移
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqLikeBatchMessage implements Serializable {

    /** BizTypeConstant: POST / COMMENT */
    private String bizType;

    private List<LikedTimesDTO> items;
}

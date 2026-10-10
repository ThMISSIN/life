package com.life.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞操作结果VO(点赞/取消统一返回)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResultVO {

    /**
     * 业务ID(帖子/评论等)
     */
    private Long bizId;

    /**
     * 业务类型: 1-帖子, 2-评论, 3-权益, 4-商品, 5-活动
     */
    private Integer bizType;

    /**
     * 操作后当前用户是否处于已点赞状态
     */
    private Boolean liked;

    /**
     * 操作后的最新点赞数
     */
    private Integer likeCount;
}

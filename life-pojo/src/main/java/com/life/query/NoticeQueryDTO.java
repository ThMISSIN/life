package com.life.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * C端通知分页查询DTO(GET /notice/page)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeQueryDTO extends QueryDTO {

    /**
     * 已读标志筛选: 0-未读, 1-已读; 不传查全部
     */
    private Integer isRead;

    /**
     * 通知类型: 1-系统, 2-订单, 3-活动, 4-权益, 5-积分; 不传查全部
     */
    private Integer type;
}

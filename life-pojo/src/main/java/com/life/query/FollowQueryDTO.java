package com.life.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 关注/粉丝分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FollowQueryDTO extends QueryDTO {

    /**
     * 列表归属者: 不传=当前登录用户本人
     */
    private Long userId;
}

package com.ruoyi.life.mapper;

import com.ruoyi.life.domain.LifeUser;

import java.util.List;

/**
 * C端用户 数据层(只读昵称/退积分)
 */
public interface LifeUserMapper
{
    /** 按ID集合批量查询用户 */
    public List<LifeUser> selectLifeUserByIds(List<Long> ids);

    /** 按ID查询 */
    public LifeUser selectLifeUserById(Long id);

    /** 加积分(作废退分, 原子自增) */
    public int addPoints(Long userId, Integer points);
}

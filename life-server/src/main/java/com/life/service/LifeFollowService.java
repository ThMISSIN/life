package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.entity.LifeFollow;
import com.life.query.FollowQueryDTO;
import com.life.result.PageResult;
import com.life.vo.FollowResultVO;

public interface LifeFollowService extends IService<LifeFollow> {

    /**
     * 关注(幂等): 无行插入 / 尸体行复活 / 已关注直接成功
     */
    FollowResultVO follow(Long followUserId);

    /**
     * 取关(幂等): 无关系也成功
     */
    FollowResultVO unfollow(Long followUserId);

    /**
     * 关注列表, queryDTO.userId不传=本人
     */
    PageResult followingList(FollowQueryDTO queryDTO);

    /**
     * 粉丝列表, queryDTO.userId不传=本人
     */
    PageResult followersList(FollowQueryDTO queryDTO);

    /**
     * 当前登录用户是否已关注指定用户
     */
    Boolean isFollowed(Long followUserId);
}

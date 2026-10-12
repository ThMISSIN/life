package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.LogicDeleteConstant;
import com.life.context.BaseContext;
import com.life.entity.LifeFollow;
import com.life.exception.UserInfoNotFoundException;
import com.life.exception.ValidationException;
import com.life.mapper.LifeFollowMapper;
import com.life.query.FollowQueryDTO;
import com.life.result.PageResult;
import com.life.service.LifeFollowService;
import com.life.service.UserService;
import com.life.vo.FollowResultVO;
import com.life.vo.FollowUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class LifeFollowServiceImpl extends ServiceImpl<LifeFollowMapper, LifeFollow> implements LifeFollowService {

    @Autowired
    private UserService userService;

    @Override
    public FollowResultVO follow(Long followUserId) {
        Long me = BaseContext.getCurrentId();
        if (me.equals(followUserId)) {
            throw new ValidationException("不能关注自己");
        }
        if (userService.getById(followUserId) == null) {
            throw new UserInfoNotFoundException("关注的用户不存在");
        }

        // 查询用户是否关注过目标用户
        LifeFollow row = baseMapper.selectByUserIdAndFollowUserId(me, followUserId);
        if (row == null) {
            // 未关注: 插入新行
            try {
                LifeFollow follow = new LifeFollow();
                follow.setUserId(me);
                follow.setFollowUserId(followUserId);
                save(follow);
            } catch (DuplicateKeyException e) {
                // 并发双击: 另一线程已插入, 本次视为幂等成功
            }
        } else if (row.getDeleted() != null && row.getDeleted() == LogicDeleteConstant.DELETED) {
            // 取关后再关注: 复活尸体行(UPDATE deleted=0, 不能插新行(必撞唯一键))
            baseMapper.reviveByUserIdAndFollowUserId(me, followUserId);
        }
        // row.deleted == 0: 已关注, 幂等直接成功
        return buildResult(followUserId, me, true);
    }

    @Override
    public FollowResultVO unfollow(Long followUserId) {
        Long me = BaseContext.getCurrentId();
        if (me.equals(followUserId)) {
            throw new ValidationException("不能取关自己");
        }
        // 逻辑删除(UPDATE deleted=1); 0行受影响=本无关系, 幂等不报错
        remove(new LambdaQueryWrapper<LifeFollow>()
                .eq(LifeFollow::getUserId, me)
                .eq(LifeFollow::getFollowUserId, followUserId));

        return buildResult(followUserId, me, false);
    }

    @Override
    public PageResult followingList(FollowQueryDTO queryDTO) {
        Long me = BaseContext.getCurrentId();
        Long owner = queryDTO.getUserId() == null ? me : queryDTO.getUserId();
        // 查询关注列表
        Page<FollowUserVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<FollowUserVO> pageResult = baseMapper.selectFollowingPage(page,owner, me);
        return new PageResult(pageResult.getTotal(), pageResult.getRecords());
    }

    @Override
    public PageResult followersList(FollowQueryDTO queryDTO) {
        Long me = BaseContext.getCurrentId();
        Long owner = queryDTO.getUserId() == null ? me : queryDTO.getUserId();
        // 查询粉丝列表
        Page<FollowUserVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<FollowUserVO> pageResult = baseMapper.selectFollowersPage(page,owner, me);
        return new PageResult(pageResult.getTotal(), pageResult.getRecords());
    }

    @Override
    public Boolean isFollowed(Long followUserId) {
        Long me = BaseContext.getCurrentId();
        if (me.equals(followUserId)) {
            return false;
        }
        return exists(new LambdaQueryWrapper<LifeFollow>()
                .eq(LifeFollow::getUserId, me)
                .eq(LifeFollow::getFollowUserId, followUserId));
    }

    // ============ 私有方法 ============

    /**
     * 关注动作后返回双方最新计数
     */
    private FollowResultVO buildResult(Long followUserId, Long me, boolean followed) {
        long followerCount = baseMapper.countFollowersValid(followUserId);
        long followingCount = baseMapper.countFollowingValid(me);
        return FollowResultVO.builder()
                .followUserId(followUserId)
                .followed(followed)
                .followerCount((int) followerCount)
                .followingCount((int) followingCount)
                .build();
    }
}

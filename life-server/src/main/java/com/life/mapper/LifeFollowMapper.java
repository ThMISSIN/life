package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifeFollow;
import com.life.vo.FollowUserVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface LifeFollowMapper extends BaseMapper<LifeFollow> {

    /**
     * 查询用户是否关注指定用户
     */
    @Select("SELECT * FROM life_follow WHERE user_id = #{userId} AND follow_user_id = #{followUserId}")
    LifeFollow selectByUserIdAndFollowUserId(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

    /**
     * 复活已逻辑删除的关系行
     */
    @Update("UPDATE life_follow SET deleted = 0, update_time = NOW() " +
            "WHERE user_id = #{userId} AND follow_user_id = #{followUserId} AND deleted = 1")
    int reviveByUserIdAndFollowUserId(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

    /**
     * 关注列表分页: owner关注的人(JOIN用户表+LEFT JOIN我→followed标记), SQL写在LifeFollowMapper.xml
     */
    Page<FollowUserVO> selectFollowingPage(Page<FollowUserVO> page,
                                           @Param("owner") Long owner, @Param("me") Long me);

    /**
     * 粉丝列表分页: 关注owner的人(JOIN用户表+LEFT JOIN我→followed标记), SQL写在LifeFollowMapper.xml
     */
    Page<FollowUserVO> selectFollowersPage(Page<FollowUserVO> page,
                                           @Param("owner") Long owner, @Param("me") Long me);

    /**
     * 有效关注数: INNER JOIN剔除"目标已注销"的关系
     */
    @Select("SELECT COUNT(*) FROM life_follow f " +
            "INNER JOIN life_user u ON u.id = f.follow_user_id AND u.deleted = 0 " +
            "WHERE f.user_id = #{userId} AND f.deleted = 0")
    long countFollowingValid(@Param("userId") Long userId);

    /**
     * 有效粉丝数: INNER JOIN剔除"粉丝已注销"的关系
     */
    @Select("SELECT COUNT(*) FROM life_follow f " +
            "INNER JOIN life_user u ON u.id = f.user_id AND u.deleted = 0 " +
            "WHERE f.follow_user_id = #{userId} AND f.deleted = 0")
    long countFollowersValid(@Param("userId") Long userId);
}

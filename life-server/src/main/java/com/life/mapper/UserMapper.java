package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.life.dto.RegisterDTO;
import com.life.entity.LifeUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper extends BaseMapper<LifeUser> {
    /**
     * 根据登录账号查询用户
     *
     * @param account
     * @return
     */
    LifeUser selectByLoginAccount(@Param("account") String account);

    void updateLoginInfo(@Param("id") Long id, @Param("clientIp") String clientIp, @Param("now") LocalDateTime now);

    int selectByUsername(@Param("username") String username);

    int selectByPhone(@Param("phone") String phone);

    int selectByEmail(@Param("email") String email);

    void saveUser(RegisterDTO registerDTO);

    @Update("update life_social.life_user set points = points - #{pointsPrice} where id = #{userId} and points >= #{pointsPrice}")
    int deductPoints(Long userId, Integer pointsPrice);

    @Update("update life_social.life_user set points = points + #{points} where id = #{userId}")
    int addPoints(Long userId, Integer points);

    @Update("update life_social.life_user set money = money - #{payAmount} where id = #{userId} and money >= #{payAmount}")
    int deductMoney(Long userId, Integer payAmount);

    @Update("update life_social.life_user set money = money + #{amount} where id = #{userId}")
    int addMoney(Long userId, Integer amount);

    /**
     * 冻结积分(2.8): 可用→冻结 单语句原子转移, 条件 points>=n 同行判定, 0行=可用不足
     */
    @Update("update life_social.life_user set points = points - #{points}, frozen_points = frozen_points + #{points} " +
            "where id = #{userId} and points >= #{points}")
    int freezePoints(@Param("userId") Long userId, @Param("points") Integer points);

    /**
     * 冻结转扣减(支付/发放成功): frozen_points>=n 条件防负
     */
    @Update("update life_social.life_user set frozen_points = frozen_points - #{points} " +
            "where id = #{userId} and frozen_points >= #{points}")
    int settleFrozen(@Param("userId") Long userId, @Param("points") Integer points);

    /**
     * 解冻回可用(取消/关单): frozen_points>=n 条件防负
     */
    @Update("update life_social.life_user set frozen_points = frozen_points - #{points}, points = points + #{points} " +
            "where id = #{userId} and frozen_points >= #{points}")
    int unfreezePoints(@Param("userId") Long userId, @Param("points") Integer points);
}

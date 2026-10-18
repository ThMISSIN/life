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
}

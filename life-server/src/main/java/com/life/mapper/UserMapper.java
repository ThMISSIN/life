package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.life.dto.RegisterDTO;
import com.life.entity.LifeUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    int selectByRegisterInfo(@Param("username") String username, @Param("phone") String phone, @Param("email") String email);

    void saveUser(RegisterDTO registerDTO);
}

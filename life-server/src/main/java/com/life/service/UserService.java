package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.dto.LoginDTO;
import com.life.dto.RegisterDTO;
import com.life.dto.UserUpdateDTO;
import com.life.entity.LifeUser;
import com.life.result.Result;
import com.life.vo.LoginVO;
import com.life.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService extends IService<LifeUser> {
    LoginVO login(LoginDTO userLoginDTO, HttpServletRequest request);

    Result register(RegisterDTO registerDTO);

    Result forget(LoginDTO loginDTO);

    Result logout(HttpServletRequest request);

    UserVO getUserInfo();

    // 用户主页公开资料(不含手机号/邮箱), 附关注/粉丝计数与当前用户关注状态
    UserVO getUserDetail(Long userId);

    Result updateUser(UserUpdateDTO userUpdateDTO);
}

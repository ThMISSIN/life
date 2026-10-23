package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.dto.LoginDTO;
import com.life.dto.RechargeDTO;
import com.life.dto.RegisterDTO;
import com.life.dto.UpdatePasswordDTO;
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

    // 模拟充值: 直接入账+余额流水(正式渠道接入后改为支付回调驱动)
    void recharge(RechargeDTO rechargeDTO);

    // 修改密码: 登录态凭原密码校验通过后更新(区别于免登录的忘记密码)
    void changePassword(UpdatePasswordDTO updatePasswordDTO);
}

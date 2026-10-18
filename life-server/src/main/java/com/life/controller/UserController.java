package com.life.controller;

import com.life.dto.LoginDTO;
import com.life.dto.RechargeDTO;
import com.life.dto.RegisterDTO;
import com.life.dto.UserUpdateDTO;
import com.life.entity.LifeUser;
import com.life.mapper.UserMapper;
import com.life.properties.JwtProperties;
import com.life.result.Result;
import com.life.service.UserService;
import com.life.vo.LoginVO;
import com.life.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * @param userLoginDTO
     * @param request
     * @return
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO userLoginDTO, HttpServletRequest request){
        LoginVO loginVo = userService.login(userLoginDTO, request);
        return Result.success(loginVo);
    }

    /**
     * 注册接口，只能使用手机号或者邮箱注册
     * @param registerDTO
     * @return
     */
    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO registerDTO) {
        return userService.register(registerDTO);
    }

    /**
     * 忘记密码
     * @param loginDTO
     * @return
     */
    @PutMapping("/forget")
    public Result forget(@RequestBody LoginDTO loginDTO) {
        return userService.forget(loginDTO);
    }

    /**
     * 用户登出，将token加入Redis黑名单直至其自然过期
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        return userService.logout(request);
    }

    /**
     * 获取当前登录用户信息
     * @return
     */
    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        UserVO userVO = userService.getUserInfo();
        return Result.success(userVO);
    }

    /**
     * 获取指定用户的公开资料(用户主页: 昵称/头像/等级/关注粉丝数/关注状态)
     * @param userId 目标用户ID
     * @return
     */
    @GetMapping("/{userId}")
    public Result<UserVO> getUserDetail(@PathVariable Long userId) {
        UserVO userVO = userService.getUserDetail(userId);
        return Result.success(userVO);
    }

    /**
     * 修改用户信息（昵称、头像、手机号、邮箱）
     * @param userUpdateDTO
     * @return
     */
    @PutMapping("/update")
    public Result updateUser(@RequestBody UserUpdateDTO userUpdateDTO) {
        return userService.updateUser(userUpdateDTO);
    }

    /**
     * 模拟充值(登录后直接入账, 无支付渠道回调)
     * @param rechargeDTO amount: 充值金额(分)
     * @return
     */
    @PostMapping("/recharge")
    public Result recharge(@RequestBody RechargeDTO rechargeDTO) {
        userService.recharge(rechargeDTO);
        return Result.success();
    }
}

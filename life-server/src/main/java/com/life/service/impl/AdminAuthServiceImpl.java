package com.life.service.impl;

import com.life.constant.JwtClaimsConstant;
import com.life.constant.RedisConstant;
import com.life.constant.StatusConstant;
import com.life.constant.UserTypeConstant;
import com.life.dto.LoginDTO;
import com.life.entity.LifeUser;
import com.life.exception.AccountLockedException;
import com.life.exception.PasswordErrorException;
import com.life.exception.UserNotLoginException;
import com.life.exception.ValidationException;
import com.life.mapper.UserMapper;
import com.life.properties.JwtProperties;
import com.life.result.Result;
import com.life.service.AdminAuthService;
import com.life.utils.JwtUtil;
import com.life.vo.LoginVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 2.3 管理端认证实现: 复用 life_user 账号体系(user_type=1), 令牌用 adminSecretKey 签发、
 * adminTokenName 头上送 —— 与C端令牌密钥隔离, 互相不可用(见 JwtAdminInterceptor)
 */
@Service
@Slf4j
public class AdminAuthServiceImpl implements AdminAuthService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public LoginVO adminLogin(LoginDTO loginDTO) {
        String account = loginDTO.getAccount() == null ? "" : loginDTO.getAccount().trim();
        if (!StringUtils.hasText(account) || !StringUtils.hasText(loginDTO.getPassword())) {
            throw new ValidationException("账号或密码不能为空");
        }
        LifeUser user = userMapper.selectByLoginAccount(account);
        // 账号不存在/非管理员统一口径, 不暴露"该账号存在但不是管理员"
        if (ObjectUtils.isEmpty(user) || user.getUserType() == null
                || user.getUserType() != UserTypeConstant.ADMIN) {
            throw new PasswordErrorException("账号或密码错误");
        }
        if (!StatusConstant.ENABLE.equals(user.getStatus())) {
            throw new AccountLockedException("账号已被禁用，请联系管理员");
        }
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new PasswordErrorException("账号或密码错误");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USER_TYPE, UserTypeConstant.ADMIN);
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        String token = JwtUtil.createJWT(adminSecret(), adminTtl(), claims);
        log.info("管理端登录成功 userId={} username={}", user.getId(), user.getUsername());
        return LoginVO.builder().id(user.getId()).token(token).build();
    }

    @Override
    public Result adminLogout(HttpServletRequest request) {
        String token = request.getHeader(adminTokenName());
        if (!StringUtils.hasText(token)) {
            throw new UserNotLoginException("未登录");
        }
        try {
            Claims claims = JwtUtil.parseJWT(adminSecret(), token);
            long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                stringRedisTemplate.opsForValue().set(
                        RedisConstant.USER_TOKEN_BLACKLIST + token,
                        String.valueOf(claims.get(JwtClaimsConstant.USER_ID)),
                        ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // 无效令牌无需拉黑, 直接视为退出成功
        }
        return Result.success("退出成功");
    }

    private String adminSecret() {
        String s = jwtProperties.getAdminSecretKey();
        return s == null || s.isBlank() ? jwtProperties.getUserSecretKey() : s;
    }

    private long adminTtl() {
        return jwtProperties.getAdminTtl() > 0 ? jwtProperties.getAdminTtl() : 7200000L;
    }

    private String adminTokenName() {
        String n = jwtProperties.getAdminTokenName();
        return n == null || n.isBlank() ? "admin-authentication" : n;
    }
}

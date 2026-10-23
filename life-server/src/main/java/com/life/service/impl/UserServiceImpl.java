package com.life.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.BalanceTypeConstant;
import com.life.constant.JwtClaimsConstant;
import com.life.constant.PasswordConstant;
import com.life.constant.RedisKeyConstant;
import com.life.constant.RegexConstant;
import com.life.constant.StatusConstant;
import com.life.context.BaseContext;
import com.life.dto.LoginDTO;
import com.life.dto.RechargeDTO;
import com.life.dto.RegisterDTO;
import com.life.dto.UpdatePasswordDTO;
import com.life.dto.UserUpdateDTO;
import com.life.entity.LifeBalanceRecord;
import com.life.entity.LifeUser;
import com.life.exception.AccountLockedException;
import com.life.exception.AccountNotFoundException;
import com.life.exception.PasswordErrorException;
import com.life.exception.UserAlreadyExistsException;
import com.life.exception.UserNotLoginException;
import com.life.exception.UserInfoNotFoundException;
import com.life.exception.ValidationException;
import com.life.mapper.LifeBalanceRecordMapper;
import com.life.mapper.UserMapper;
import com.life.properties.JwtProperties;
import com.life.result.Result;
import com.life.service.UserService;
import com.life.utils.JwtUtil;
import com.life.vo.LoginVO;
import com.life.vo.UserVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, LifeUser> implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private com.life.mapper.LifeFollowMapper lifeFollowMapper;
    @Autowired
    private LifeBalanceRecordMapper balanceRecordMapper;

    @Override
    public LoginVO login(LoginDTO userLoginDTO, HttpServletRequest request) {
        String account = userLoginDTO.getAccount().trim();
        String password = userLoginDTO.getPassword();
        // 1.参数校验
        if (!StringUtils.hasText(account)
                || !StringUtils.hasText(password)) {
            throw new ValidationException("账号或密码不能为空");
        }

        // 2.查询账号是否存在
        LifeUser user = userMapper.selectByLoginAccount(account);

        if (ObjectUtils.isEmpty(user)) {
            throw new PasswordErrorException("账号或密码错误");
        }

        // 3.账号状态校验
        if (!StatusConstant.ENABLE.equals(user.getStatus())) {
            throw new AccountLockedException("账号已被禁用，请联系管理员");
        }

        // 4.密码校验
        if (!BCrypt.checkpw(userLoginDTO.getPassword(),user.getPassword())) {
            throw new PasswordErrorException("账号或密码错误");
        }

        //5.更新登录信息：登录次数+1、ip、登录时间
        String clientIp = getClientIp(request);
        userMapper.updateLoginInfo(user.getId(), clientIp, LocalDateTime.now());

        //6.生成token
        Map<String, Object> claims=new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID,user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        //7.组装VO返回
        return LoginVO.builder()
                .id(user.getId())
                .token(token)
                .build();
    }

    @Override
    public Result register(RegisterDTO registerDTO) {
        String username = registerDTO.getUsername();
        String phone = registerDTO.getPhone();
        String email = registerDTO.getEmail();
        String rawPassword = registerDTO.getPassword();

        // 必填项非空校验
        if (username == null || username.isBlank()) {
            throw new ValidationException("用户名不能为空");
        }
        if (phone == null || phone.isBlank()) {
            throw new ValidationException("手机号不能为空");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ValidationException("密码不能为空");
        }

        // trim处理
        username = username.trim();
        phone = phone.trim();
        registerDTO.setUsername(username);
        registerDTO.setPhone(phone);

        // 校验手机号
        String phoneRegex = RegexConstant.PHONE;
        boolean b1 = phone.trim().matches(phoneRegex);
        if (!b1) {
            throw new ValidationException("手机号格式有误");
        }

        // 校验邮箱（非必填，填写时需校验格式）
        if (email != null && !email.isBlank()) {
            String emailRegex = RegexConstant.EMAIL;
            boolean b2 = email.trim().matches(emailRegex);
            if (!b2) {
                throw new ValidationException("邮箱格式有误");
            }
        }

        // 验证用户是否存在，存在则不允许重复创建
        if (userMapper.selectByUsername(username) != 0) {
            throw new UserAlreadyExistsException("用户名已存在");
        }
        if (userMapper.selectByPhone(phone) != 0) {
            throw new UserAlreadyExistsException("手机号已注册");
        }
        if (email != null && !email.isBlank() && userMapper.selectByEmail(email) != 0) {
            throw new UserAlreadyExistsException("邮箱已被使用");
        }

        registerDTO.setPassword(BCrypt.hashpw(rawPassword,BCrypt.gensalt(PasswordConstant.HASH_COST)));

        // 新增用户
        userMapper.saveUser(registerDTO);

        return Result.success("注册成功");
    }

    @Override
    public Result forget(LoginDTO loginDTO) {
        String account = loginDTO.getAccount();
        String password = loginDTO.getPassword();
        String confirmPassword = loginDTO.getConfirmPassword();

        // 1.参数校验
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new ValidationException("账号或密码不能为空");
        }

        // 2.两次密码一致性校验
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("两次输入的密码不一致");
        }

        // 3.查询账号是否存在（用户名/手机号/邮箱）
        LifeUser user = userMapper.selectByLoginAccount(account.trim());
        if (ObjectUtils.isEmpty(user)) {
            throw new AccountNotFoundException("账号不存在");
        }

        // 4.账号状态校验
        if (!StatusConstant.ENABLE.equals(user.getStatus())) {
            throw new AccountLockedException("账号已被禁用，请联系管理员");
        }

        // 5.重置密码
        LifeUser updateUser = new LifeUser();
        updateUser.setId(user.getId());
        updateUser.setPassword(BCrypt.hashpw(password, BCrypt.gensalt(PasswordConstant.HASH_COST)));
        userMapper.updateById(updateUser);

        return Result.success("密码重置成功");
    }

    /**
     * 1. 用户登录 → 服务端生成 token A → 返回给客户端
     * 2. 用户登出 → token A 加入 Redis 黑名单（失效）
     * 3. 用户再次登录 → 服务端生成 token B（全新的字符串）→ 返回给客户端
     * token A 和 token B 是完全不同的字符串，黑名单里只有 token A，不影响 token B 通过校验。
     * @param request
     * @return
     */
    @Override
    public Result logout(HttpServletRequest request) {
        // 1.从请求头中获取token，未携带则视为未登录
        String token = request.getHeader(jwtProperties.getUserTokenName());
        if (!StringUtils.hasText(token)) {
            throw new UserNotLoginException("用户未登录");
        }

        // 2.解析token，已失效（过期/伪造）的token无需加入黑名单，直接视为退出成功
        Claims claims;
        try {
            claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
        } catch (Exception e) {
            return Result.success("退出成功");
        }

        // 3.将token加入黑名单，过期时间与token剩余有效期一致，到期自动清除
        long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstant.USER_TOKEN_BLACKLIST + token,
                    String.valueOf(claims.get(JwtClaimsConstant.USER_ID)),
                    ttl, TimeUnit.MILLISECONDS);
        }

        return Result.success("退出成功");
    }

    /** 获取客户端真实IP */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    public UserVO getUserInfo() {
        Long userId = BaseContext.getCurrentId();
        LifeUser user = this.getById(userId);
        if (user == null) {
            throw new UserInfoNotFoundException("用户不存在");
        }
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setPhone(user.getPhone());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar());
        userVO.setEmail(user.getEmail());
        userVO.setUserType(user.getUserType());
        userVO.setPoints(user.getPoints());
        userVO.setLevel(user.getLevel());
        userVO.setStatus(user.getStatus());
        userVO.setLastLoginTime(user.getLastLoginTime());
        userVO.setLoginCount(user.getLoginCount());
        return userVO;
    }

    @Override
    public UserVO getUserDetail(Long userId) {
        LifeUser user = this.getById(userId);
        if (user == null) {
            throw new UserInfoNotFoundException("用户不存在");
        }
        UserVO userVO = new UserVO();
        // 公开资料: 不含手机号/邮箱等敏感字段
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar());
        userVO.setUserType(user.getUserType());
        userVO.setPoints(user.getPoints());
        userVO.setLevel(user.getLevel());
        userVO.setFollowingCount((int) lifeFollowMapper.countFollowingValid(userId));
        userVO.setFollowerCount((int) lifeFollowMapper.countFollowersValid(userId));

        Long me = BaseContext.getCurrentId();
        if (me != null && !me.equals(userId)) {
            userVO.setFollowed(lifeFollowMapper.exists(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.life.entity.LifeFollow>()
                    .eq(com.life.entity.LifeFollow::getUserId, me)
                    .eq(com.life.entity.LifeFollow::getFollowUserId, userId)));
        }
        return userVO;
    }

    @Override
    public Result updateUser(UserUpdateDTO userUpdateDTO) {
        Long userId = BaseContext.getCurrentId();
        LifeUser user = new LifeUser();
        user.setId(userId);
        user.setNickname(userUpdateDTO.getNickname());
        user.setAvatar(userUpdateDTO.getAvatar());
        user.setPhone(userUpdateDTO.getPhone());
        user.setEmail(userUpdateDTO.getEmail());
        this.updateById(user);
        return Result.success("修改成功");
    }

    /**
     * 模拟充值: 直接加余额+流水(type=1)
     * 单笔上限防前端填错/滥用; 正式接入渠道后入账应由支付回调驱动而非本接口
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(RechargeDTO rechargeDTO) {
        Integer amount = rechargeDTO.getAmount();
        if (amount == null || amount <= 0 || amount > 5_000_000) {
            throw new ValidationException("充值金额需在0.01~50000元之间");
        }
        Long userId = BaseContext.getCurrentId();
        userMapper.addMoney(userId, amount);
        balanceRecordMapper.insert(LifeBalanceRecord.builder()
                .userId(userId)
                .amount(amount)
                .type(BalanceTypeConstant.RECHARGE)
                .remark("模拟充值")
                .build());
    }

    /**
     * 修改密码: 登录态下凭原密码修改; 校验链 参数非空->原密码正确->新旧不同
     * 只更新password列(updateById部分更新), 避免整行覆盖
     */
    @Override
    public void changePassword(UpdatePasswordDTO updatePasswordDTO) {
        if (!StringUtils.hasText(updatePasswordDTO.getOldPassword())
                || !StringUtils.hasText(updatePasswordDTO.getNewPassword())) {
            throw new ValidationException("原密码和新密码不能为空");
        }
        if (updatePasswordDTO.getOldPassword().equals(updatePasswordDTO.getNewPassword())) {
            throw new ValidationException("新密码不能与原密码相同");
        }
        Long userId = BaseContext.getCurrentId();
        LifeUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new ValidationException("用户不存在");
        }
        if (!BCrypt.checkpw(updatePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new PasswordErrorException("原密码错误");
        }

        LifeUser update = new LifeUser();
        update.setId(userId);
        update.setPassword(BCrypt.hashpw(updatePasswordDTO.getNewPassword(), BCrypt.gensalt(PasswordConstant.HASH_COST)));
        userMapper.updateById(update);
    }
}

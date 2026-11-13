package com.ruoyi.life.domain;

import java.util.Date;

/**
 * C端用户对象 life_user（管理端仅读, 用于昵称/积分展示与作废退分）
 *
 * @author life
 */
public class LifeUser extends LifeBaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 登录账号 */
    private String username;

    /** 手机号 */
    private String phone;

    /** 密码(不对外下发) */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 邮箱 */
    private String email;

    /** 用户类型 */
    private Integer userType;

    /** 积分 */
    private Integer points;

    /** 余额(分) */
    private Integer money;

    /** 会员等级 */
    private Integer level;

    /** 状态 */
    private Integer status;

    /** 最后登录时间 */
    private Date lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 登录次数 */
    private Integer loginCount;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public Integer getUserType()
    {
        return userType;
    }

    public void setUserType(Integer userType)
    {
        this.userType = userType;
    }

    public Integer getPoints()
    {
        return points;
    }

    public void setPoints(Integer points)
    {
        this.points = points;
    }

    public Integer getMoney()
    {
        return money;
    }

    public void setMoney(Integer money)
    {
        this.money = money;
    }

    public Integer getLevel()
    {
        return level;
    }

    public void setLevel(Integer level)
    {
        this.level = level;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Date getLastLoginTime()
    {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime)
    {
        this.lastLoginTime = lastLoginTime;
    }

    public String getLastLoginIp()
    {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp)
    {
        this.lastLoginIp = lastLoginIp;
    }

    public Integer getLoginCount()
    {
        return loginCount;
    }

    public void setLoginCount(Integer loginCount)
    {
        this.loginCount = loginCount;
    }
}

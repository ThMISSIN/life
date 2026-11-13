package com.ruoyi.life.util;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * life业务公共填充与口径工具
 */
public class LifeUtils
{
    /** 当前登录的若依用户ID(审计用) */
    public static Long currentOperator()
    {
        try
        {
            return SecurityUtils.getUserId();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** 当前时间 */
    public static java.util.Date now()
    {
        return DateUtils.getNowDate();
    }

    /** 起始时间归一化: 仅日期时补 00:00:00 */
    public static String startAt(String time)
    {
        if (time == null || time.isEmpty())
        {
            return null;
        }
        return time.length() == 10 ? time + " 00:00:00" : time;
    }

    /** 截止时间归一化: 仅日期时补 23:59:59 */
    public static String endAt(String time)
    {
        if (time == null || time.isEmpty())
        {
            return null;
        }
        return time.length() == 10 ? time + " 23:59:59" : time;
    }
}

package com.ruoyi.life.domain;

import com.ruoyi.common.annotation.Excel;
import java.util.Date;

/**
 * 积分流水对象 life_points_record
 *
 * @author life
 */
public class LifePointsRecord extends LifeBaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户昵称(查询展示字段, 非表字段) */
    @Excel(name = "用户昵称")
    private String nickname;

    /** 积分值(正数为获取, 负数为消耗) */
    @Excel(name = "积分值")
    private Integer points;

    /** 积分类型: 1-签到, 2-消费, 3-活动奖励, 4-邀请好友, 5-评价奖励, 6-积分兑换, 7-兑换作废退回, 8-排行奖励 */
    @Excel(name = "积分类型")
    private Integer type;

    /** 关联订单号 */
    @Excel(name = "关联订单号")
    private String orderNo;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public Integer getPoints()
    {
        return points;
    }

    public void setPoints(Integer points)
    {
        this.points = points;
    }

    public Integer getType()
    {
        return type;
    }

    public void setType(Integer type)
    {
        this.type = type;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}

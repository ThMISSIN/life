package com.ruoyi.life.domain;

import com.ruoyi.common.annotation.Excel;
import java.util.Date;

/**
 * 权益对象 life_benefit
 *
 * @author life
 */
public class LifeBenefit extends LifeBaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 权益名称 */
    @Excel(name = "权益名称")
    private String name;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称(查询展示字段, 非表字段) */
    @Excel(name = "所属分类")
    private String categoryName;

    /** 权益描述 */
    private String description;

    /** 封面图 */
    private String coverImage;

    /** 详情图(JSON数组) */
    private String images;

    /** 权益类型: 1-券码类, 2-直充类 */
    @Excel(name = "权益类型", readConverterExp = "1=券码类,2=直充类")
    private Integer benefitType;

    /** 积分价格 */
    @Excel(name = "积分价格")
    private Integer pointsPrice;

    /** 原价(分) */
    @Excel(name = "原价(分)")
    private Integer originalPrice;

    /** 有效期类型: 1-相对天数, 2-固定截止日 */
    private Integer validType;

    /** 有效天数 */
    private Integer validDays;

    /** 固定截止时间 */
    private Date fixedEndTime;

    /** 每人限兑次数, 0-不限 */
    @Excel(name = "限兑次数")
    private Integer perUserLimit;

    /** 可兑换的最低会员等级, 0-不限 */
    private Integer minLevel;

    /** 库存 */
    @Excel(name = "库存")
    private Integer stock;

    /** 销量 */
    @Excel(name = "销量")
    private Integer sales;

    /** 状态: 0-下架, 1-上架 */
    @Excel(name = "状态", readConverterExp = "0=下架,1=上架")
    private Integer status;

    /** 排序号 */
    private Integer sort;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getCoverImage()
    {
        return coverImage;
    }

    public void setCoverImage(String coverImage)
    {
        this.coverImage = coverImage;
    }

    public String getImages()
    {
        return images;
    }

    public void setImages(String images)
    {
        this.images = images;
    }

    public Integer getBenefitType()
    {
        return benefitType;
    }

    public void setBenefitType(Integer benefitType)
    {
        this.benefitType = benefitType;
    }

    public Integer getPointsPrice()
    {
        return pointsPrice;
    }

    public void setPointsPrice(Integer pointsPrice)
    {
        this.pointsPrice = pointsPrice;
    }

    public Integer getOriginalPrice()
    {
        return originalPrice;
    }

    public void setOriginalPrice(Integer originalPrice)
    {
        this.originalPrice = originalPrice;
    }

    public Integer getValidType()
    {
        return validType;
    }

    public void setValidType(Integer validType)
    {
        this.validType = validType;
    }

    public Integer getValidDays()
    {
        return validDays;
    }

    public void setValidDays(Integer validDays)
    {
        this.validDays = validDays;
    }

    public Date getFixedEndTime()
    {
        return fixedEndTime;
    }

    public void setFixedEndTime(Date fixedEndTime)
    {
        this.fixedEndTime = fixedEndTime;
    }

    public Integer getPerUserLimit()
    {
        return perUserLimit;
    }

    public void setPerUserLimit(Integer perUserLimit)
    {
        this.perUserLimit = perUserLimit;
    }

    public Integer getMinLevel()
    {
        return minLevel;
    }

    public void setMinLevel(Integer minLevel)
    {
        this.minLevel = minLevel;
    }

    public Integer getStock()
    {
        return stock;
    }

    public void setStock(Integer stock)
    {
        this.stock = stock;
    }

    public Integer getSales()
    {
        return sales;
    }

    public void setSales(Integer sales)
    {
        this.sales = sales;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }
}

package com.ruoyi.life.domain;

import com.ruoyi.common.annotation.Excel;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 商品对象 life_product（双通道: 积分兑换+现金购买）
 *
 * @author life
 */
public class LifeProduct extends LifeBaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String name;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称(查询展示字段, 非表字段) */
    @Excel(name = "所属分类")
    private String categoryName;

    /** 商品描述 */
    private String description;

    /** 封面图 */
    private String coverImage;

    /** 详情图(JSON数组) */
    private String images;

    /** 标签(逗号分隔) */
    private String tags;

    /** 店铺名称 */
    @Excel(name = "店铺名称")
    private String shopName;

    /** 发货地 */
    private String location;

    /** 现金价格(分), 0-不可现金购买 */
    @Excel(name = "现金价格(分)")
    private Integer price;

    /** 原价(分), 划线价 */
    @Excel(name = "原价(分)")
    private Integer originalPrice;

    /** 积分价格, 0-不可积分兑换 */
    @Excel(name = "积分价格")
    private Integer pointsPrice;

    /** 每人限兑次数(积分通道), 0-不限 */
    @Excel(name = "限兑次数")
    private Integer perUserLimit;

    /** 积分兑换的最低会员等级, 0-不限 */
    private Integer minLevel;

    /** 库存, -1表示不限量 */
    @Excel(name = "库存")
    private Integer stock;

    /** 销量 */
    @Excel(name = "销量")
    private Integer sales;

    /** 状态: 0-下架, 1-上架 */
    @Excel(name = "状态", readConverterExp = "0=下架,1=上架")
    private Integer status;

    /** 排序号 */
    @Excel(name = "排序号")
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

    public String getTags()
    {
        return tags;
    }

    public void setTags(String tags)
    {
        this.tags = tags;
    }

    public String getShopName()
    {
        return shopName;
    }

    public void setShopName(String shopName)
    {
        this.shopName = shopName;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public Integer getPrice()
    {
        return price;
    }

    public void setPrice(Integer price)
    {
        this.price = price;
    }

    public Integer getOriginalPrice()
    {
        return originalPrice;
    }

    public void setOriginalPrice(Integer originalPrice)
    {
        this.originalPrice = originalPrice;
    }

    public Integer getPointsPrice()
    {
        return pointsPrice;
    }

    public void setPointsPrice(Integer pointsPrice)
    {
        this.pointsPrice = pointsPrice;
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

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("categoryId", getCategoryId())
            .append("status", getStatus())
            .toString();
    }
}

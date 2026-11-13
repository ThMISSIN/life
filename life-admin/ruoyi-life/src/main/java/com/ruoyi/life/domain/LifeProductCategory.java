package com.ruoyi.life.domain;

/**
 * 商品分类对象 life_product_category
 *
 * @author life
 */
public class LifeProductCategory extends LifeBaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类图标 */
    private String icon;

    /** 父分类ID, 0表示顶级分类 */
    private Long parentId;

    /** 分类层级: 1-一级, 2-二级 */
    private Integer level;

    /** 排序号 */
    private Integer sort;

    /** 状态: 0-禁用, 1-正常 */
    private Integer status;

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

    public String getIcon()
    {
        return icon;
    }

    public void setIcon(String icon)
    {
        this.icon = icon;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public Integer getLevel()
    {
        return level;
    }

    public void setLevel(Integer level)
    {
        this.level = level;
    }

    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }
}

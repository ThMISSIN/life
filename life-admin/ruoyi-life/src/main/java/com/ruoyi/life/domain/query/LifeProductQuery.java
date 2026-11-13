package com.ruoyi.life.domain.query;

/**
 * 商品分页查询（迁移自C端 ProductQueryDTO, 管理端口径: status不传查全部）
 */
public class LifeProductQuery
{
    /** 分类ID */
    private Long categoryId;

    /** 关键词, 按商品名称/店铺名模糊搜索 */
    private String keyword;

    /** 排序方式: 1-综合(默认,按sort), 2-销量, 3-价格升序, 4-价格降序 */
    private Integer sortType;

    /** 状态: 0-下架, 1-上架; 不传查全部 */
    private Integer status;

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public Integer getSortType()
    {
        return sortType;
    }

    public void setSortType(Integer sortType)
    {
        this.sortType = sortType;
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

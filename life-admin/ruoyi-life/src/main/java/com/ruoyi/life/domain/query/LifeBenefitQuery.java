package com.ruoyi.life.domain.query;

/**
 * 权益分页查询（迁移自C端 BenefitQueryDTO, 管理端口径: status不传查全部）
 */
public class LifeBenefitQuery
{
    /** 状态: 0-下架, 1-上架; 不传查全部 */
    private Integer status;

    /** 分类ID */
    private Long categoryId;

    /** 关键词, 按权益名称模糊搜索 */
    private String keyword;

    /** 排序方式: 1-综合(默认,按sort), 2-销量, 3-积分价格升序, 4-积分价格降序 */
    private Integer sortType;

    /** 权益类型: 1-券码, 2-直充; 不传查全部 */
    private Integer benefitType;

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

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

    public Integer getBenefitType()
    {
        return benefitType;
    }

    public void setBenefitType(Integer benefitType)
    {
        this.benefitType = benefitType;
    }
}

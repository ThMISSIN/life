package com.ruoyi.web.controller.life;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.life.domain.LifeBenefitCategory;
import com.ruoyi.life.service.ILifeBenefitCategoryService;

/**
 * 权益分类管理（管理端接口, 自C端 life-java AdminBenefitCategoryController 迁移）
 * 补充 /list 全量查询接口, 供权益页分类下拉与分类页表格使用
 *
 * @author life
 */
@RestController
@RequestMapping("/life/benefitCategory")
public class LifeBenefitCategoryController extends BaseController
{
    @Autowired
    private ILifeBenefitCategoryService lifeBenefitCategoryService;

    /**
     * 全量分类列表(管理端含禁用, 不分页)
     */
    @PreAuthorize("@ss.hasPermi('life:benefitCategory:list')")
    @GetMapping("/list")
    public AjaxResult list(LifeBenefitCategory query)
    {
        return success(lifeBenefitCategoryService.selectLifeBenefitCategoryList());
    }

    /**
     * 分类详情
     */
    @PreAuthorize("@ss.hasPermi('life:benefitCategory:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(lifeBenefitCategoryService.selectLifeBenefitCategoryById(id));
    }

    /**
     * 新增分类
     */
    @Log(title = "权益分类管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('life:benefitCategory:add')")
    @PostMapping
    public AjaxResult add(@RequestBody LifeBenefitCategory lifeBenefitCategory)
    {
        return toAjax(lifeBenefitCategoryService.insertLifeBenefitCategory(lifeBenefitCategory));
    }

    /**
     * 修改分类
     */
    @Log(title = "权益分类管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:benefitCategory:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody LifeBenefitCategory lifeBenefitCategory)
    {
        return toAjax(lifeBenefitCategoryService.updateLifeBenefitCategory(lifeBenefitCategory));
    }

    /**
     * 删除分类(软删除, 存在子分类或挂载权益时禁止)
     */
    @Log(title = "权益分类管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('life:benefitCategory:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(lifeBenefitCategoryService.deleteLifeBenefitCategoryById(id));
    }
}

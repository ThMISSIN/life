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
import com.ruoyi.life.domain.LifeProductCategory;
import com.ruoyi.life.service.ILifeProductCategoryService;

/**
 * 商品分类管理（管理端接口, 自C端 life-java AdminProductCategoryController 迁移）
 * 补充 /list 全量查询接口, 供商品页分类下拉与分类页表格使用
 *
 * @author life
 */
@RestController
@RequestMapping("/life/productCategory")
public class LifeProductCategoryController extends BaseController
{
    @Autowired
    private ILifeProductCategoryService lifeProductCategoryService;

    /**
     * 全量分类列表(管理端含禁用, 不分页)
     */
    @PreAuthorize("@ss.hasPermi('life:productCategory:list')")
    @GetMapping("/list")
    public AjaxResult list(LifeProductCategory query)
    {
        return success(lifeProductCategoryService.selectLifeProductCategoryList());
    }

    /**
     * 分类详情
     */
    @PreAuthorize("@ss.hasPermi('life:productCategory:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(lifeProductCategoryService.selectLifeProductCategoryById(id));
    }

    /**
     * 新增分类
     */
    @Log(title = "商品分类管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('life:productCategory:add')")
    @PostMapping
    public AjaxResult add(@RequestBody LifeProductCategory lifeProductCategory)
    {
        return toAjax(lifeProductCategoryService.insertLifeProductCategory(lifeProductCategory));
    }

    /**
     * 修改分类
     */
    @Log(title = "商品分类管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:productCategory:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody LifeProductCategory lifeProductCategory)
    {
        return toAjax(lifeProductCategoryService.updateLifeProductCategory(lifeProductCategory));
    }

    /**
     * 删除分类(软删除, 存在子分类或挂载商品时禁止)
     */
    @Log(title = "商品分类管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('life:productCategory:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(lifeProductCategoryService.deleteLifeProductCategoryById(id));
    }
}

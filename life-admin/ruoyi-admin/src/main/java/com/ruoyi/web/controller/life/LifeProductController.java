package com.ruoyi.web.controller.life;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.life.domain.LifeProduct;
import com.ruoyi.life.domain.query.LifeProductQuery;
import com.ruoyi.life.service.ILifeProductService;

/**
 * 商品管理（管理端接口, 自C端 life-java AdminProductController 迁移）
 *
 * @author life
 */
@RestController
@RequestMapping("/life/product")
public class LifeProductController extends BaseController
{
    @Autowired
    private ILifeProductService lifeProductService;

    /**
     * 商品分页查询(可见下架数据, status不传查全部)
     */
    @PreAuthorize("@ss.hasPermi('life:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(LifeProductQuery query)
    {
        startPage();
        List<LifeProduct> list = lifeProductService.selectLifeProductList(query);
        return getDataTable(list);
    }

    /**
     * 商品导出
     */
    @Log(title = "商品管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('life:product:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, LifeProductQuery query)
    {
        List<LifeProduct> list = lifeProductService.selectLifeProductList(query);
        ExcelUtil<LifeProduct> util = new ExcelUtil<LifeProduct>(LifeProduct.class);
        util.exportExcel(response, list, "商品数据");
    }

    /**
     * 商品详情
     */
    @PreAuthorize("@ss.hasPermi('life:product:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(lifeProductService.selectLifeProductById(id));
    }

    /**
     * 新增商品
     */
    @Log(title = "商品管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('life:product:add')")
    @PostMapping
    public AjaxResult add(@RequestBody LifeProduct lifeProduct)
    {
        return toAjax(lifeProductService.insertLifeProduct(lifeProduct));
    }

    /**
     * 修改商品
     */
    @Log(title = "商品管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:product:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody LifeProduct lifeProduct)
    {
        return toAjax(lifeProductService.updateLifeProduct(lifeProduct));
    }

    /**
     * 上下架(原 /admin/product/{id}/status/{status})
     */
    @Log(title = "商品管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:product:edit')")
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestParam Long id, @RequestParam Integer status)
    {
        return toAjax(lifeProductService.changeStatus(id, status));
    }

    /**
     * 删除商品(软删除, 上架中禁止)
     */
    @Log(title = "商品管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('life:product:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lifeProductService.deleteLifeProductByIds(ids));
    }
}

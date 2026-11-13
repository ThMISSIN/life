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
import com.ruoyi.life.domain.LifeBenefit;
import com.ruoyi.life.domain.query.LifeBenefitQuery;
import com.ruoyi.life.service.ILifeBenefitService;

/**
 * 权益管理（管理端接口, 自C端 life-java AdminBenefitController 迁移）
 *
 * @author life
 */
@RestController
@RequestMapping("/life/benefit")
public class LifeBenefitController extends BaseController
{
    @Autowired
    private ILifeBenefitService lifeBenefitService;

    /**
     * 权益分页查询(可见下架数据, status不传查全部)
     */
    @PreAuthorize("@ss.hasPermi('life:benefit:list')")
    @GetMapping("/list")
    public TableDataInfo list(LifeBenefitQuery query)
    {
        startPage();
        List<LifeBenefit> list = lifeBenefitService.selectLifeBenefitList(query);
        return getDataTable(list);
    }

    /**
     * 权益导出
     */
    @Log(title = "权益管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('life:benefit:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, LifeBenefitQuery query)
    {
        List<LifeBenefit> list = lifeBenefitService.selectLifeBenefitList(query);
        ExcelUtil<LifeBenefit> util = new ExcelUtil<LifeBenefit>(LifeBenefit.class);
        util.exportExcel(response, list, "权益数据");
    }

    /**
     * 权益详情
     */
    @PreAuthorize("@ss.hasPermi('life:benefit:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(lifeBenefitService.selectLifeBenefitById(id));
    }

    /**
     * 新增权益
     */
    @Log(title = "权益管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('life:benefit:add')")
    @PostMapping
    public AjaxResult add(@RequestBody LifeBenefit lifeBenefit)
    {
        return toAjax(lifeBenefitService.insertLifeBenefit(lifeBenefit));
    }

    /**
     * 修改权益
     */
    @Log(title = "权益管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:benefit:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody LifeBenefit lifeBenefit)
    {
        return toAjax(lifeBenefitService.updateLifeBenefit(lifeBenefit));
    }

    /**
     * 上下架(原 /admin/benefit/{id}/status/{status})
     */
    @Log(title = "权益管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:benefit:edit')")
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestParam Long id, @RequestParam Integer status)
    {
        return toAjax(lifeBenefitService.changeStatus(id, status));
    }

    /**
     * 删除权益(软删除, 存在待生效/未使用发放记录时禁止)
     */
    @Log(title = "权益管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('life:benefit:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lifeBenefitService.deleteLifeBenefitByIds(ids));
    }
}

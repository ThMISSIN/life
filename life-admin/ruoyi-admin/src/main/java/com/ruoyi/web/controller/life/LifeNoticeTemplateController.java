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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.life.domain.LifeNoticeTemplate;
import com.ruoyi.life.domain.query.LifeNoticeTemplateQuery;
import com.ruoyi.life.service.ILifeNoticeTemplateService;

/**
 * 通知模板管理(1.3 管理端新增)
 *
 * @author life
 */
@RestController
@RequestMapping("/life/noticeTemplate")
public class LifeNoticeTemplateController extends BaseController
{
    @Autowired
    private ILifeNoticeTemplateService lifeNoticeTemplateService;

    /**
     * 模板分页查询(status不传查全部)
     */
    @PreAuthorize("@ss.hasPermi('life:noticeTemplate:list')")
    @GetMapping("/list")
    public TableDataInfo list(LifeNoticeTemplateQuery query)
    {
        startPage();
        List<LifeNoticeTemplate> list = lifeNoticeTemplateService.selectLifeNoticeTemplateList(query);
        return getDataTable(list);
    }

    /**
     * 模板详情
     */
    @PreAuthorize("@ss.hasPermi('life:noticeTemplate:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(lifeNoticeTemplateService.selectLifeNoticeTemplateById(id));
    }

    /**
     * 新增模板
     */
    @Log(title = "通知模板管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('life:noticeTemplate:add')")
    @PostMapping
    public AjaxResult add(@RequestBody LifeNoticeTemplate lifeNoticeTemplate)
    {
        return toAjax(lifeNoticeTemplateService.insertLifeNoticeTemplate(lifeNoticeTemplate));
    }

    /**
     * 修改模板
     */
    @Log(title = "通知模板管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:noticeTemplate:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody LifeNoticeTemplate lifeNoticeTemplate)
    {
        return toAjax(lifeNoticeTemplateService.updateLifeNoticeTemplate(lifeNoticeTemplate));
    }

    /**
     * 启用/禁用
     */
    @Log(title = "通知模板管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:noticeTemplate:edit')")
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestParam Long id, @RequestParam Integer status)
    {
        return toAjax(lifeNoticeTemplateService.changeStatus(id, status));
    }

    /**
     * 删除模板(软删除)
     */
    @Log(title = "通知模板管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('life:noticeTemplate:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        int rows = 0;
        for (Long id : ids)
        {
            rows += lifeNoticeTemplateService.deleteLifeNoticeTemplateById(id);
        }
        return toAjax(rows);
    }
}

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
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.life.domain.LifeAnnouncement;
import com.ruoyi.life.domain.query.LifeAnnouncementQuery;
import com.ruoyi.life.service.ILifeAnnouncementService;

/**
 * 系统公告管理(1.3 管理端新增): 公告CRUD + 发布/下架(撤回)
 * 发布动作经 Redis Pub/Sub 通知C端 life-social 做收件箱fan-out与WebSocket实时推送
 *
 * @author life
 */
@RestController
@RequestMapping("/life/announcement")
public class LifeAnnouncementController extends BaseController
{
    @Autowired
    private ILifeAnnouncementService lifeAnnouncementService;

    /**
     * 公告分页查询(status不传查全部, 含草稿)
     */
    @PreAuthorize("@ss.hasPermi('life:announcement:list')")
    @GetMapping("/list")
    public TableDataInfo list(LifeAnnouncementQuery query)
    {
        startPage();
        List<LifeAnnouncement> list = lifeAnnouncementService.selectLifeAnnouncementList(query);
        return getDataTable(list);
    }

    /**
     * 公告详情
     */
    @PreAuthorize("@ss.hasPermi('life:announcement:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(lifeAnnouncementService.selectLifeAnnouncementById(id));
    }

    /**
     * 新增公告(默认草稿, 表单可选直接发布)
     */
    @Log(title = "系统公告管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('life:announcement:add')")
    @PostMapping
    public AjaxResult add(@RequestBody LifeAnnouncement lifeAnnouncement)
    {
        return toAjax(lifeAnnouncementService.insertLifeAnnouncement(lifeAnnouncement));
    }

    /**
     * 修改公告(已发布需先撤回)
     */
    @Log(title = "系统公告管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:announcement:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody LifeAnnouncement lifeAnnouncement)
    {
        return toAjax(lifeAnnouncementService.updateLifeAnnouncement(lifeAnnouncement));
    }

    /**
     * 发布公告(草稿/已撤回 -> 已发布, 触发C端收件箱fan-out)
     */
    @Log(title = "系统公告管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:announcement:publish')")
    @PutMapping("/publish/{id}")
    public AjaxResult publish(@PathVariable Long id)
    {
        return toAjax(lifeAnnouncementService.publish(id));
    }

    /**
     * 下架/撤回公告(已发布 -> 已撤回, C端收件箱已fan-out的副本保留, 公告位仅下架展示)
     */
    @Log(title = "系统公告管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:announcement:publish')")
    @PutMapping("/revoke/{id}")
    public AjaxResult revoke(@PathVariable Long id)
    {
        return toAjax(lifeAnnouncementService.revoke(id));
    }

    /**
     * 删除公告(软删除, 已发布禁止)
     */
    @Log(title = "系统公告管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('life:announcement:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        int rows = 0;
        for (Long id : ids)
        {
            rows += lifeAnnouncementService.deleteLifeAnnouncementById(id);
        }
        return toAjax(rows);
    }
}

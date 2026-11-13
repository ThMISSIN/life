package com.ruoyi.web.controller.life;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.life.domain.LifePointsRecord;
import com.ruoyi.life.domain.query.LifeRecordQuery;
import com.ruoyi.life.service.ILifePointsRecordService;

/**
 * 积分流水管理（管理端接口, 自C端 life-java AdminPointsRecordController 迁移）
 *
 * @author life
 */
@RestController
@RequestMapping("/life/pointsRecord")
public class LifePointsRecordController extends BaseController
{
    @Autowired
    private ILifePointsRecordService lifePointsRecordService;

    /**
     * 积分流水分页(按用户/类型/时间区间筛选, 时间降序)
     */
    @PreAuthorize("@ss.hasPermi('life:pointsRecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(LifeRecordQuery query)
    {
        startPage();
        List<LifePointsRecord> list = lifePointsRecordService.selectLifePointsRecordList(query);
        return getDataTable(list);
    }

    /**
     * 积分流水导出
     */
    @Log(title = "积分流水", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('life:pointsRecord:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, LifeRecordQuery query)
    {
        List<LifePointsRecord> list = lifePointsRecordService.selectLifePointsRecordList(query);
        ExcelUtil<LifePointsRecord> util = new ExcelUtil<LifePointsRecord>(LifePointsRecord.class);
        util.exportExcel(response, list, "积分流水");
    }
}

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
import com.ruoyi.life.domain.LifeBalanceRecord;
import com.ruoyi.life.domain.query.LifeRecordQuery;
import com.ruoyi.life.service.ILifeBalanceRecordService;

/**
 * 余额流水管理（管理端接口, 自C端 life-java AdminBalanceRecordController 迁移）
 *
 * @author life
 */
@RestController
@RequestMapping("/life/balanceRecord")
public class LifeBalanceRecordController extends BaseController
{
    @Autowired
    private ILifeBalanceRecordService lifeBalanceRecordService;

    /**
     * 余额流水分页(按用户/类型/时间区间筛选, 时间降序)
     */
    @PreAuthorize("@ss.hasPermi('life:balanceRecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(LifeRecordQuery query)
    {
        startPage();
        List<LifeBalanceRecord> list = lifeBalanceRecordService.selectLifeBalanceRecordList(query);
        return getDataTable(list);
    }

    /**
     * 余额流水导出
     */
    @Log(title = "余额流水", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('life:balanceRecord:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, LifeRecordQuery query)
    {
        List<LifeBalanceRecord> list = lifeBalanceRecordService.selectLifeBalanceRecordList(query);
        ExcelUtil<LifeBalanceRecord> util = new ExcelUtil<LifeBalanceRecord>(LifeBalanceRecord.class);
        util.exportExcel(response, list, "余额流水");
    }
}

package com.ruoyi.web.controller.life;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.life.domain.LifeOrder;
import com.ruoyi.life.domain.query.LifeOrderQuery;
import com.ruoyi.life.service.ILifeOrderService;

/**
 * 订单管理（管理端接口, 自C端 life-java AdminOrderController 迁移）
 *
 * @author life
 */
@RestController
@RequestMapping("/life/order")
public class LifeOrderController extends BaseController
{
    @Autowired
    private ILifeOrderService lifeOrderService;

    /**
     * 订单分页查询(含下单人昵称与订单明细)
     */
    @PreAuthorize("@ss.hasPermi('life:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(LifeOrderQuery query)
    {
        startPage();
        List<LifeOrder> list = lifeOrderService.selectLifeOrderList(query);
        return getDataTable(list);
    }

    /**
     * 订单导出
     */
    @Log(title = "订单管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('life:order:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, LifeOrderQuery query)
    {
        List<LifeOrder> list = lifeOrderService.selectLifeOrderList(query);
        ExcelUtil<LifeOrder> util = new ExcelUtil<LifeOrder>(LifeOrder.class);
        util.exportExcel(response, list, "订单数据");
    }

    /**
     * 订单详情(含明细)
     */
    @PreAuthorize("@ss.hasPermi('life:order:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(lifeOrderService.selectLifeOrderById(id));
    }

    /**
     * 发货(仅已支付订单, 原 /admin/order/{id}/deliver)
     */
    @Log(title = "订单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:order:deliver')")
    @PutMapping("/deliver/{id}")
    public AjaxResult deliver(@PathVariable Long id)
    {
        return toAjax(lifeOrderService.deliverOrder(id));
    }
}

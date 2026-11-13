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
import com.ruoyi.life.domain.LifeUserBenefit;
import com.ruoyi.life.domain.query.LifeUserBenefitQuery;
import com.ruoyi.life.service.ILifeUserBenefitService;

/**
 * 用户权益(发放记录)管理（管理端接口, 自C端 life-java AdminUserBenefitController 迁移）
 *
 * @author life
 */
@RestController
@RequestMapping("/life/userBenefit")
public class LifeUserBenefitController extends BaseController
{
    @Autowired
    private ILifeUserBenefitService lifeUserBenefitService;

    /**
     * 发放记录分页: 可按userId/benefitId/status/orderNo/code筛选, 含券码
     */
    @PreAuthorize("@ss.hasPermi('life:userBenefit:list')")
    @GetMapping("/list")
    public TableDataInfo list(LifeUserBenefitQuery query)
    {
        startPage();
        List<LifeUserBenefit> list = lifeUserBenefitService.selectLifeUserBenefitList(query);
        return getDataTable(list);
    }

    /**
     * 发放记录导出
     */
    @Log(title = "发放记录", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('life:userBenefit:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, LifeUserBenefitQuery query)
    {
        List<LifeUserBenefit> list = lifeUserBenefitService.selectLifeUserBenefitList(query);
        ExcelUtil<LifeUserBenefit> util = new ExcelUtil<LifeUserBenefit>(LifeUserBenefit.class);
        util.exportExcel(response, list, "权益发放记录");
    }

    /**
     * 作废发放记录: 券作废 + 退积分(写流水) + 回库存减销量 (原 /admin/benefit/user/{id}/void)
     */
    @Log(title = "发放记录", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:userBenefit:void')")
    @PutMapping("/void/{id}")
    public AjaxResult voidBenefit(@PathVariable Long id)
    {
        return toAjax(lifeUserBenefitService.voidUserBenefit(id));
    }

    /**
     * 商家按券码核销(1.7, 原拟 PUT /admin/benefit/user/verify/{code}):
     * 校验存在/已生效/未使用/未过期后置已使用, 返回发放记录供小票展示;
     * 与C端用户自核销共用状态口径, verify_operator 记管理端操作员
     */
    @Log(title = "发放记录核销", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('life:userBenefit:verify')")
    @PutMapping("/verify/{code}")
    public AjaxResult verifyByCode(@PathVariable("code") String code)
    {
        return success(lifeUserBenefitService.verifyByCode(code));
    }
}

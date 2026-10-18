package com.life.controller.admin;

import com.life.query.UserBenefitQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifeUserBenefitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端用户权益(发放记录)接口
 */
@RestController
@RequestMapping("/admin/benefit/user")
public class AdminUserBenefitController {

    @Autowired
    private LifeUserBenefitService lifeUserBenefitService;

    /**
     * 发放记录分页: 可按userId/benefitId/status/orderNo/code筛选, 含券码
     */
    @GetMapping("/page")
    public Result<PageResult> page(UserBenefitQueryDTO query) {
        return Result.success(lifeUserBenefitService.queryAdminPage(query));
    }

    /**
     * 作废发放记录: 券作废 + 退积分(写流水) + 回库存减销量
     */
    @PutMapping("/{id}/void")
    public Result voidBenefit(@PathVariable Long id) {
        lifeUserBenefitService.voidUserBenefit(id);
        return Result.success();
    }
}

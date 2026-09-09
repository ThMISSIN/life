package com.life.controller;

import com.life.entity.LifeBenefit;
import com.life.result.Result;
import com.life.service.LifeBenefitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权益管理端接口
 */
@RestController
@RequestMapping("/admin/benefit")
public class AdminBenefitController {

    @Autowired
    private LifeBenefitService lifeBenefitService;

    /**
     * 新增权益
     */
    @PostMapping
    public Result add(@RequestBody LifeBenefit lifeBenefit) {
        lifeBenefitService.addBenefit(lifeBenefit);
        return Result.success();
    }

    /**
     * 删除权益(软删除)
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        lifeBenefitService.removeBenefit(id);
        return Result.success();
    }

    /**
     * 修改权益
     */
    @PutMapping
    public Result update(@RequestBody LifeBenefit lifeBenefit) {
        lifeBenefitService.updateById(lifeBenefit);
        return Result.success();
    }
}

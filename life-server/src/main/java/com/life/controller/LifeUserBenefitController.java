package com.life.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifeUserBenefit;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifeUserBenefitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/benefit/user")
public class LifeUserBenefitController {

    @Autowired
    private LifeUserBenefitService lifeUserBenefitService;

    /**
     * 新增用户权益
     */
    @PostMapping
    public Result add(@RequestBody LifeUserBenefit lifeUserBenefit) {
        lifeUserBenefitService.save(lifeUserBenefit);
        return Result.success();
    }

    /**
     * 删除用户权益
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        lifeUserBenefitService.removeById(id);
        return Result.success();
    }

    /**
     * 修改用户权益
     */
    @PutMapping
    public Result update(@RequestBody LifeUserBenefit lifeUserBenefit) {
        lifeUserBenefitService.updateById(lifeUserBenefit);
        return Result.success();
    }

    /**
     * 根据ID查询用户权益
     */
    @GetMapping("/{id}")
    public Result<LifeUserBenefit> getById(@PathVariable Long id) {
        LifeUserBenefit lifeUserBenefit = lifeUserBenefitService.getById(id);
        return Result.success(lifeUserBenefit);
    }

    /**
     * 分页查询用户权益
     */
    @GetMapping("/page")
    public Result<PageResult> page(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<LifeUserBenefit> pageParam = new Page<>(page, pageSize);
        lifeUserBenefitService.page(pageParam);
        PageResult pageResult = new PageResult(pageParam.getTotal(), pageParam.getRecords());
        return Result.success(pageResult);
    }
}

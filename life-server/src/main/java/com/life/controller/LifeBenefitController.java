package com.life.controller;

import com.life.query.BenefitQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifeBenefitService;
import com.life.vo.BenefitDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 权益 C端接口: 浏览/详情
 */
@RestController
@RequestMapping("/benefit")
public class LifeBenefitController {

    @Autowired
    private LifeBenefitService lifeBenefitService;

    /**
     * 根据ID查询权益详情
     */
    @GetMapping("/{id}")
    public Result<BenefitDetailVO> getById(@PathVariable Long id) {
        BenefitDetailVO detail = lifeBenefitService.getBenefitDetail(id);
        return Result.success(detail);
    }

    /**
     * 分页查询已上架权益
     */
    @GetMapping("/page")
    public Result<PageResult> page(BenefitQueryDTO query) {
        PageResult pageResult = lifeBenefitService.pageList(query);
        return Result.success(pageResult);
    }
}

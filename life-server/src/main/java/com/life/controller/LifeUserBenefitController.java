package com.life.controller;

import com.life.query.UserBenefitQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifeUserBenefitService;
import com.life.vo.UserBenefitVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * C端「我的权益」接口: 分页/详情(含券码)/核销
 * 原 /benefit/user/* 通用CRUD已下线, 用户权益仅能由下单权益通道发放
 */
@RestController
@RequestMapping("/benefit/mine")
public class LifeUserBenefitController {

    @Autowired
    private LifeUserBenefitService lifeUserBenefitService;

    /**
     * 分页查询我的权益(可按状态筛选, 列表不含券码)
     */
    @GetMapping("/page")
    public Result<PageResult> page(UserBenefitQueryDTO query) {
        return Result.success(lifeUserBenefitService.queryMyPage(query));
    }

    /**
     * 我的权益详情(含券码, 供出示核销)
     */
    @GetMapping("/{id}")
    public Result<UserBenefitVO> detail(@PathVariable Long id) {
        return Result.success(lifeUserBenefitService.getMyDetail(id));
    }

    /**
     * 核销(使用)我的权益
     */
    @PutMapping("/{id}/verify")
    public Result verify(@PathVariable Long id) {
        lifeUserBenefitService.verifyMyBenefit(id);
        return Result.success();
    }
}

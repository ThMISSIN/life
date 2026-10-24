package com.life.controller;

import com.life.query.QueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifePointsRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C端积分流水接口(需登录, /points/** 已注册拦截器)
 *
 * 流水表是 append-only 的对账事实源: 只应由签到/兑换/退款等业务动作作为副作用写入
 * (统一入口见 roadmap 2.2 changePoints 重构), 不提供任何直接增/删/改端点——
 * 原通用 CRUD 脚手架属伪造/越权篡改账本的后门, 已于 2026-10-24 移除。
 * 管理端全量视角走 /admin/points/record/page (AdminPointsRecordController)。
 */
@RestController
@RequestMapping("/points/record")
public class LifePointsRecordController {

    @Autowired
    private LifePointsRecordService lifePointsRecordService;

    /**
     * 分页查询当前用户积分流水(服务层从登录态取 userId, 仅本人视角; 前端积分明细在用)
     */
    @GetMapping("/page")
    public Result<PageResult> page(QueryDTO queryDTO) {
        PageResult pageResult = lifePointsRecordService.pageList(queryDTO);
        return Result.success(pageResult);
    }
}

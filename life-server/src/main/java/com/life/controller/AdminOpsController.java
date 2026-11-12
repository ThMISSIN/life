package com.life.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifeRiskRecord;
import com.life.exception.ValidationException;
import com.life.mapper.LifeRiskRecordMapper;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.PostSearchService;
import com.life.service.StockCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运维/管理支撑接口(/admin/** 独立鉴权):
 * ES 全量重建(2.11) / 库存镜像预热(2.14) / 风控记录查询(2.10)
 */
@RestController
@RequestMapping("/admin/ops")
@RequiredArgsConstructor
public class AdminOpsController {

    private final PostSearchService postSearchService;
    private final StockCacheService stockCacheService;
    private final LifeRiskRecordMapper riskRecordMapper;

    /**
     * ES 全量重建索引: 双写漂移/换集群/首次接入时使用, 分页扫描已发布帖 _bulk 灌入
     */
    @PostMapping("/search/reindex")
    public Result<Integer> reindex() {
        return Result.success(postSearchService.reindexAll());
    }

    /**
     * 库存镜像预热: itemType=product|benefit; 活动开始前手动预热热点商品
     * (下单链路对未预热商品也会按需自动预热, 本接口用于提前压量)
     */
    @PostMapping("/stock/preheat")
    public Result<Boolean> preheat(@RequestParam String itemType, @RequestParam Long itemId,
                                   @RequestParam Integer stock) {
        if (!StockCacheService.TYPE_PRODUCT.equals(itemType)
                && !StockCacheService.TYPE_BENEFIT.equals(itemType)) {
            throw new ValidationException("itemType 仅支持 product/benefit");
        }
        return Result.success(stockCacheService.preheat(itemType, itemId, stock));
    }

    /**
     * 风控记录分页: userId/actionType 可选筛选, 时间倒序(拦截率统计与误伤排查)
     */
    @GetMapping("/risk/page")
    public Result<PageResult> riskPage(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) Long userId,
                                       @RequestParam(required = false) String actionType) {
        Page<LifeRiskRecord> page = riskRecordMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<LifeRiskRecord>()
                        .eq(userId != null, LifeRiskRecord::getUserId, userId)
                        .eq(actionType != null && !actionType.isBlank(), LifeRiskRecord::getActionType, actionType)
                        .orderByDesc(LifeRiskRecord::getCreateTime));
        return Result.success(new PageResult(page.getTotal(), page.getRecords()));
    }
}

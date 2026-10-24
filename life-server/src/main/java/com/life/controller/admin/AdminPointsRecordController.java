package com.life.controller.admin;

import com.life.query.RecordQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifePointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端积分流水接口(挂 /admin/**, 已在拦截器内)
 * 与 C 端 /points/record/page(仅本人视角)区分: 此处全量视角, 对账依赖
 */
@RestController
@RequestMapping("/admin/points/record")
@RequiredArgsConstructor
public class AdminPointsRecordController {

    private final LifePointsRecordService pointsRecordService;

    /**
     * 积分流水分页(按用户/类型/时间区间筛选, 时间降序)
     */
    @GetMapping("/page")
    public Result<PageResult> page(RecordQueryDTO query) {
        return Result.success(pointsRecordService.queryAdminPage(query));
    }
}

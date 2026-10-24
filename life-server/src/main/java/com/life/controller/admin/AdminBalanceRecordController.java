package com.life.controller.admin;

import com.life.query.RecordQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifeBalanceRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端余额流水接口(挂 /admin/**, 已在拦截器内)
 * 对账依赖: life_balance_record 此前只有写入口(充值/支付/退款), 无查询入口
 */
@RestController
@RequestMapping("/admin/balance/record")
@RequiredArgsConstructor
public class AdminBalanceRecordController {

    private final LifeBalanceRecordService balanceRecordService;

    /**
     * 余额流水分页(按用户/类型/时间区间筛选, 时间降序)
     */
    @GetMapping("/page")
    public Result<PageResult> page(RecordQueryDTO query) {
        return Result.success(balanceRecordService.queryAdminPage(query));
    }
}

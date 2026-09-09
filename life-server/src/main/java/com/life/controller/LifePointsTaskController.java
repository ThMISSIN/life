package com.life.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifePointsTask;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifePointsTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/points/task")
public class LifePointsTaskController {

    @Autowired
    private LifePointsTaskService lifePointsTaskService;

    /**
     * 新增积分任务
     */
    @PostMapping
    public Result add(@RequestBody LifePointsTask lifePointsTask) {
        lifePointsTaskService.save(lifePointsTask);
        return Result.success();
    }

    /**
     * 删除积分任务
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        lifePointsTaskService.removeById(id);
        return Result.success();
    }

    /**
     * 修改积分任务
     */
    @PutMapping
    public Result update(@RequestBody LifePointsTask lifePointsTask) {
        lifePointsTaskService.updateById(lifePointsTask);
        return Result.success();
    }

    /**
     * 根据ID查询积分任务
     */
    @GetMapping("/{id}")
    public Result<LifePointsTask> getById(@PathVariable Long id) {
        LifePointsTask lifePointsTask = lifePointsTaskService.getById(id);
        return Result.success(lifePointsTask);
    }

    /**
     * 分页查询积分任务
     */
    @GetMapping("/page")
    public Result<PageResult> page(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<LifePointsTask> pageParam = new Page<>(page, pageSize);
        lifePointsTaskService.page(pageParam);
        PageResult pageResult = new PageResult(pageParam.getTotal(), pageParam.getRecords());
        return Result.success(pageResult);
    }
}

package com.life.controller;


import com.life.entity.LifePointsRecord;
import com.life.query.QueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.LifePointsRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/points/record")
public class LifePointsRecordController {

    @Autowired
    private LifePointsRecordService lifePointsRecordService;

    /**
     * 新增积分记录
     */
    @PostMapping
    public Result add(@RequestBody LifePointsRecord lifePointsRecord) {
        lifePointsRecordService.save(lifePointsRecord);
        return Result.success();
    }

    /**
     * 删除积分记录
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        lifePointsRecordService.removeById(id);
        return Result.success();
    }

    /**
     * 修改积分记录
     */
    @PutMapping
    public Result update(@RequestBody LifePointsRecord lifePointsRecord) {
        lifePointsRecordService.updateById(lifePointsRecord);
        return Result.success();
    }

    /**
     * 根据ID查询积分记录
     */
    @GetMapping("/{id}")
    public Result<LifePointsRecord> getById(@PathVariable Long id) {
        LifePointsRecord lifePointsRecord = lifePointsRecordService.getById(id);
        return Result.success(lifePointsRecord);
    }

    /**
     * 分页查询积分记录
     */
    @GetMapping("/page")
    public Result<PageResult> page(QueryDTO queryDTO) {
        PageResult pageResult = lifePointsRecordService.pageList(queryDTO);
        return Result.success(pageResult);
    }
}

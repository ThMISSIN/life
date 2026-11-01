package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.entity.LifePointsTask;
import com.life.mapper.LifePointsTaskMapper;
import com.life.query.QueryDTO;
import com.life.result.PageResult;
import com.life.service.LifePointsTaskService;
import com.life.utils.BeanUtils;
import com.life.vo.PointsTaskVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LifePointsTaskServiceImpl extends ServiceImpl<LifePointsTaskMapper, LifePointsTask> implements LifePointsTaskService {

    @Override
    public PageResult queryCEndPage(QueryDTO query) {
        LocalDateTime now = LocalDateTime.now();
        // 上架 + 时间窗内(start/end 未配置视为不限), id升序保持运营排布
        LambdaQueryWrapper<LifePointsTask> wrapper = new LambdaQueryWrapper<LifePointsTask>()
                .eq(LifePointsTask::getStatus, 1)
                .and(w -> w.isNull(LifePointsTask::getStartTime).or().le(LifePointsTask::getStartTime, now))
                .and(w -> w.isNull(LifePointsTask::getEndTime).or().ge(LifePointsTask::getEndTime, now))
                .orderByAsc(LifePointsTask::getId);
        Page<LifePointsTask> page = this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        // 只下发展示字段, 不暴露审计/时间窗配置
        List<PointsTaskVO> vos = BeanUtils.copyList(page.getRecords(), PointsTaskVO.class);
        return new PageResult(page.getTotal(), vos);
    }
}

package com.life.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.entity.LifePointsTask;
import com.life.mapper.LifePointsTaskMapper;
import com.life.service.LifePointsTaskService;
import org.springframework.stereotype.Service;

@Service
public class LifePointsTaskServiceImpl extends ServiceImpl<LifePointsTaskMapper, LifePointsTask> implements LifePointsTaskService {
}

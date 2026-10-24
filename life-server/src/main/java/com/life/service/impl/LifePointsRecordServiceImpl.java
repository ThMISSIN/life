package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.PointsTypeConstant;
import com.life.context.BaseContext;
import com.life.entity.LifePointsRecord;
import com.life.entity.LifeUser;
import com.life.mapper.LifePointsRecordMapper;
import com.life.mapper.UserMapper;
import com.life.query.QueryDTO;
import com.life.query.RecordQueryDTO;
import com.life.result.PageResult;
import com.life.service.LifePointsRecordService;
import com.life.utils.BeanUtils;
import com.life.vo.PointsRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LifePointsRecordServiceImpl extends ServiceImpl<LifePointsRecordMapper, LifePointsRecord> implements LifePointsRecordService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResult pageList(QueryDTO queryDTO) {
        Long userId = BaseContext.getCurrentId();

        Page<LifePointsRecord> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<LifePointsRecord> resultPage = baseMapper.queryList(page, userId, queryDTO);

        return new PageResult(resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    public void addSignRecord(Long userId, int points) {
        LifePointsRecord record = LifePointsRecord.builder()
                .userId(userId)
                .points(points)
                .type(PointsTypeConstant.SIGN)
                .build();
        save(record);
    }

    @Override
    public PageResult queryAdminPage(RecordQueryDTO query) {
        // 管理端全量流水: 按用户/类型/时间区间筛选, 时间降序(口径对齐管理端订单分页)
        LambdaQueryWrapper<LifePointsRecord> wrapper = new LambdaQueryWrapper<LifePointsRecord>()
                .eq(query.getUserId() != null, LifePointsRecord::getUserId, query.getUserId())
                .eq(query.getType() != null, LifePointsRecord::getType, query.getType())
                .ge(query.getStartTime() != null, LifePointsRecord::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, LifePointsRecord::getCreateTime, query.getEndTime())
                .orderByDesc(LifePointsRecord::getCreateTime);
        Page<LifePointsRecord> page = this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<PointsRecordVO> vos = BeanUtils.copyList(page.getRecords(), PointsRecordVO.class);
        // 关联昵称: 批量查询一次, 避免逐行查(与 queryAdminOrderPage 同款模式)
        if (!vos.isEmpty()) {
            Set<Long> userIds = vos.stream().map(PointsRecordVO::getUserId).collect(Collectors.toSet());
            Map<Long, String> nameMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(LifeUser::getId,
                            u -> u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername()));
            vos.forEach(vo -> vo.setNickname(nameMap.get(vo.getUserId())));
        }
        return new PageResult(page.getTotal(), vos);
    }
}

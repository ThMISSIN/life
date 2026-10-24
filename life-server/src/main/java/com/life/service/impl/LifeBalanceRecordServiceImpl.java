package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.entity.LifeBalanceRecord;
import com.life.entity.LifeUser;
import com.life.mapper.LifeBalanceRecordMapper;
import com.life.mapper.UserMapper;
import com.life.query.RecordQueryDTO;
import com.life.result.PageResult;
import com.life.service.LifeBalanceRecordService;
import com.life.utils.BeanUtils;
import com.life.vo.BalanceRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LifeBalanceRecordServiceImpl extends ServiceImpl<LifeBalanceRecordMapper, LifeBalanceRecord>
        implements LifeBalanceRecordService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResult queryAdminPage(RecordQueryDTO query) {
        // 管理端全量流水: 按用户/类型/时间区间筛选, 时间降序(口径对齐管理端订单分页)
        LambdaQueryWrapper<LifeBalanceRecord> wrapper = new LambdaQueryWrapper<LifeBalanceRecord>()
                .eq(query.getUserId() != null, LifeBalanceRecord::getUserId, query.getUserId())
                .eq(query.getType() != null, LifeBalanceRecord::getType, query.getType())
                .ge(query.getStartTime() != null, LifeBalanceRecord::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, LifeBalanceRecord::getCreateTime, query.getEndTime())
                .orderByDesc(LifeBalanceRecord::getCreateTime);
        Page<LifeBalanceRecord> page = this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<BalanceRecordVO> vos = BeanUtils.copyList(page.getRecords(), BalanceRecordVO.class);
        // 关联昵称: 批量查询一次, 避免逐行查(与 queryAdminOrderPage 同款模式)
        if (!vos.isEmpty()) {
            Set<Long> userIds = vos.stream().map(BalanceRecordVO::getUserId).collect(Collectors.toSet());
            Map<Long, String> nameMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(LifeUser::getId,
                            u -> u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername()));
            vos.forEach(vo -> vo.setNickname(nameMap.get(vo.getUserId())));
        }
        return new PageResult(page.getTotal(), vos);
    }
}

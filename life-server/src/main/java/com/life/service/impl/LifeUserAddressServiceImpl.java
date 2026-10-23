package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.RegexConstant;
import com.life.context.BaseContext;
import com.life.dto.AddressDTO;
import com.life.entity.LifeUserAddress;
import com.life.exception.ValidationException;
import com.life.mapper.LifeUserAddressMapper;
import com.life.service.LifeUserAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LifeUserAddressServiceImpl extends ServiceImpl<LifeUserAddressMapper, LifeUserAddress> implements LifeUserAddressService {

    @Override
    public List<LifeUserAddress> listMyAddresses() {
        return this.list(new LambdaQueryWrapper<LifeUserAddress>()
                .eq(LifeUserAddress::getUserId, BaseContext.getCurrentId())
                .orderByDesc(LifeUserAddress::getIsDefault, LifeUserAddress::getCreateTime));
    }

    /**
     * 新增: 无任何地址时首个强制默认(保证下单流程不因"无默认"卡住), 设默认时互斥清除其他
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAddress(AddressDTO addressDTO) {
        Long userId = BaseContext.getCurrentId();
        checkParams(addressDTO);
        // 查询用户是否有默认地址, 无则设为默认
        long count = this.count(new LambdaQueryWrapper<LifeUserAddress>()
                .eq(LifeUserAddress::getUserId, userId));
        // 没有地址 或 设为默认时, 才设为默认
        int isDefault = (count == 0 || Integer.valueOf(1).equals(addressDTO.getIsDefault())) ? 1 : 0;
        if (isDefault == 1) {
            // 清除其他默认地址
            clearOtherDefault(userId, null);
        }
        // 整理参数
        LifeUserAddress address = new LifeUserAddress();
        address.setUserId(userId);
        address.setReceiverName(addressDTO.getReceiverName().trim());
        address.setReceiverPhone(addressDTO.getReceiverPhone().trim());
        address.setProvince(addressDTO.getProvince().trim());
        address.setCity(addressDTO.getCity().trim());
        address.setDistrict(addressDTO.getDistrict().trim());
        address.setAddress(addressDTO.getAddress().trim());
        address.setIsDefault(isDefault);
        save(address);
    }

    /**
     * 修改: 仅支持把本条设为默认(传isDefault=1), 不支持经编辑取消默认——保证用户始终至多一个默认地址
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(AddressDTO addressDTO) {
        if (addressDTO.getId() == null) {
            throw new ValidationException("地址ID不能为空");
        }
        checkParams(addressDTO);
        Long userId = BaseContext.getCurrentId();
        LifeUserAddress existing = requireOwn(addressDTO.getId(), userId);

        if (Integer.valueOf(1).equals(addressDTO.getIsDefault())) {
            clearOtherDefault(userId, existing.getId());
        }

        LifeUserAddress update = new LifeUserAddress();
        update.setId(existing.getId());
        update.setReceiverName(addressDTO.getReceiverName().trim());
        update.setReceiverPhone(addressDTO.getReceiverPhone().trim());
        update.setProvince(addressDTO.getProvince().trim());
        update.setCity(addressDTO.getCity().trim());
        update.setDistrict(addressDTO.getDistrict().trim());
        update.setAddress(addressDTO.getAddress().trim());
        if (Integer.valueOf(1).equals(addressDTO.getIsDefault())) {
            update.setIsDefault(1);
        }
        updateById(update);
    }

    /**
     * 删除: 删掉默认地址不做自动递补, 前端引导用户重设即可
     */
    @Override
    public void deleteAddress(Long id) {
        requireOwn(id, BaseContext.getCurrentId());
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long id) {
        Long userId = BaseContext.getCurrentId();
        LifeUserAddress existing = requireOwn(id, userId);
        clearOtherDefault(userId, id);

        LifeUserAddress update = new LifeUserAddress();
        update.setId(existing.getId());
        update.setIsDefault(1);
        updateById(update);
    }

    /**
     * 归属校验: 不存在/非本人统一报不存在, 防探测他人地址(id被下单快照引用)
     */
    private LifeUserAddress requireOwn(Long id, Long userId) {
        LifeUserAddress address = getById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new ValidationException("收货地址不存在");
        }
        return address;
    }

    private void clearOtherDefault(Long userId, Long keepId) {
        List<LifeUserAddress> others = this.list(new LambdaQueryWrapper<LifeUserAddress>()
                .eq(LifeUserAddress::getUserId, userId)
                .eq(LifeUserAddress::getIsDefault, 1)
                .ne(keepId != null, LifeUserAddress::getId, keepId));
        for (LifeUserAddress other : others) {
            LifeUserAddress update = new LifeUserAddress();
            update.setId(other.getId());
            update.setIsDefault(0);
            updateById(update);
        }
    }

    private void checkParams(AddressDTO dto) {
        if (isBlank(dto.getReceiverName()) || isBlank(dto.getReceiverPhone()) || isBlank(dto.getProvince())
                || isBlank(dto.getCity()) || isBlank(dto.getDistrict()) || isBlank(dto.getAddress())) {
            throw new ValidationException("收货人、手机号、省市区、详细地址均不能为空");
        }
        if (!dto.getReceiverPhone().trim().matches(RegexConstant.PHONE)) {
            throw new ValidationException("收货人手机号格式不正确");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

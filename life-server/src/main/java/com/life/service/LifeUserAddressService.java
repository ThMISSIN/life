package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.dto.AddressDTO;
import com.life.entity.LifeUserAddress;

import java.util.List;

public interface LifeUserAddressService extends IService<LifeUserAddress> {

    /**
     * 我的地址列表(默认地址在前, 其余按创建时间倒序)
     */
    List<LifeUserAddress> listMyAddresses();

    /**
     * 新增地址(首个地址强制设为默认)
     */
    void addAddress(AddressDTO addressDTO);

    /**
     * 修改地址
     */
    void updateAddress(AddressDTO addressDTO);

    /**
     * 删除地址(逻辑删)
     */
    void deleteAddress(Long id);

    /**
     * 设为默认(互斥清除其他默认)
     */
    void setDefaultAddress(Long id);
}

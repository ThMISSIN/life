package com.life.controller;

import com.life.dto.AddressDTO;
import com.life.entity.LifeUserAddress;
import com.life.result.Result;
import com.life.service.LifeUserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端收货地址(均需登录, /user/** 已注册拦截器)
 */
@RestController
@RequestMapping("/user/address")
@RequiredArgsConstructor
public class UserAddressController {

    private final LifeUserAddressService lifeUserAddressService;

    /**
     * 地址列表(默认在前)
     */
    @GetMapping("/list")
    public Result<List<LifeUserAddress>> list() {
        return Result.success(lifeUserAddressService.listMyAddresses());
    }

    /**
     * 新增地址
     */
    @PostMapping
    public Result add(@RequestBody AddressDTO addressDTO) {
        lifeUserAddressService.addAddress(addressDTO);
        return Result.success();
    }

    /**
     * 修改地址(body带id)
     */
    @PutMapping
    public Result update(@RequestBody AddressDTO addressDTO) {
        lifeUserAddressService.updateAddress(addressDTO);
        return Result.success();
    }

    /**
     * 删除地址(逻辑删)
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        lifeUserAddressService.deleteAddress(id);
        return Result.success();
    }

    /**
     * 设为默认地址
     */
    @PutMapping("/{id}/default")
    public Result setDefault(@PathVariable Long id) {
        lifeUserAddressService.setDefaultAddress(id);
        return Result.success();
    }
}

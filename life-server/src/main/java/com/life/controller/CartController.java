package com.life.controller;

import com.life.dto.CartAddDTO;
import com.life.result.Result;
import com.life.service.CartService;
import com.life.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端购物车接口(均需登录, /cart/** 已注册拦截器) — 路由与出入参已定, 核心逻辑见 CartServiceImpl TODO
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 加购(同款重复加购合并数量)
     */
    @PostMapping
    public Result add(@RequestBody CartAddDTO addDTO) {
        cartService.addCart(addDTO);
        return Result.success();
    }

    /**
     * 购物车列表(含商品实时价格/库存/上架状态, 失效行 valid=false)
     */
    @GetMapping("/list")
    public Result<List<CartVO>> list() {
        return Result.success(cartService.queryCartList());
    }

    /**
     * 修改数量
     */
    @PutMapping("/{id}")
    public Result updateQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        cartService.updateQuantity(id, quantity);
        return Result.success();
    }

    /**
     * 删除单行
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        cartService.deleteCart(id);
        return Result.success();
    }

    /**
     * 清空购物车
     */
    @DeleteMapping
    public Result clear() {
        cartService.clearCart();
        return Result.success();
    }
}

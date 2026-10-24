package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.dto.CartAddDTO;
import com.life.entity.LifeCart;
import com.life.vo.CartVO;

import java.util.List;

/**
 * 购物车服务(C端, 均需登录) — 接口契约已定, 核心实现见 CartServiceImpl 内 TODO
 */
public interface CartService extends IService<LifeCart> {

    /**
     * 加购: 商品存在/上架/库存校验; 同用户同商品已存在则合并数量
     */
    void addCart(CartAddDTO addDTO);

    /**
     * 购物车列表: 当前用户全量行(加购时间降序), 组装商品实时价格/库存/上架状态
     */
    List<CartVO> queryCartList();

    /**
     * 改数量: 归属校验 + quantity>0 + 不超实时库存
     */
    void updateQuantity(Long id, Integer quantity);

    /**
     * 删除单行: 归属校验(非本人统一报不存在, 防探测)
     */
    void deleteCart(Long id);

    /**
     * 清空当前用户购物车(幂等: 无行也成功)
     */
    void clearCart();
}

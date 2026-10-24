package com.life.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.BenefitConstant;
import com.life.constant.ProductConstant;
import com.life.constant.RedisKeyConstant;
import com.life.constant.StatusConstant;
import com.life.context.BaseContext;
import com.life.dto.CartAddDTO;
import com.life.entity.LifeBenefit;
import com.life.entity.LifeCart;
import com.life.entity.LifeProduct;
import com.life.entity.LifeUser;
import com.life.exception.ValidationException;
import com.life.mapper.LifeBenefitMapper;
import com.life.mapper.LifeCartMapper;
import com.life.mapper.LifeProductMapper;
import com.life.service.CartService;
import com.life.service.LifeBenefitService;
import com.life.service.LifeProductService;
import com.life.service.UserService;
import com.life.utils.BeanUtils;
import com.life.utils.ObjectUtils;
import com.life.vo.CartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.hutool.core.util.NumberUtil.nullToZero;
import static com.life.constant.RedisKeyConstant.ROW_MAX_QUANTITY;

/**
 * 购物车服务实现
 *
 * 设计要点:
 *   加购/改数量的商品校验复用 LifeProductService.validatePurchase / LifeBenefitService.validateExchange(与下单同口径);
 *   结算不设独立接口: 前端勾选行组装 items 直接调 POST /order(唯一下单入口),
 *   勾选行ID经 OrderSubmitDTO.cartIds 传入, 由 submitOrder 在下单同事务内清理购物车行
 *   (当前用户ID统一走 BaseContext.getCurrentId(), 无需注入)
 */
@Service
public class CartServiceImpl extends ServiceImpl<LifeCartMapper, LifeCart> implements CartService {

    @Autowired
    private LifeProductService productService;
    @Autowired
    private LifeProductMapper productMapper;
    @Autowired
    private LifeBenefitService benefitService;
    @Autowired
    private LifeBenefitMapper benefitMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private LifeCartMapper cartMapper;

    @Override
    // TODO 锁后续仍可以优化
    public void addCart(CartAddDTO addDTO) {
        Integer itemType = addDTO.getItemType();
        Long itemId = addDTO.getItemId();
        // 1. 参数校验: itemType ∈ {1,2}, itemId 非空, quantity 为空默认1且必须 > 0
        if (itemType == null || (itemType != 1 && itemType != 2)) {
            throw new ValidationException("产品类型不合法");
        }
        if (itemId == null) {
            throw new ValidationException("商品ID不能为空");
        }
        int quantity = addDTO.getQuantity() == null ? 1 : addDTO.getQuantity();
        if (quantity <= 0) {
            throw new ValidationException("商品数量不正确");
        }
        //  2. 商品校验
        Long userId = BaseContext.getCurrentId();
        LifeUser user = userService.getById(userId);
        if (user == null || !StatusConstant.ENABLE.equals(user.getStatus())) {
            throw new ValidationException("用户不存在或已禁用");
        }
        // 3. 分类型校验 下架、过期、删除的商品当场拒绝
        String itemName;
        String itemImage;
        int price;
        int pointsPrice;
        if (addDTO.getItemType() == 1) {
            LifeProduct product = productService.validatePurchase(addDTO.getItemId(), quantity, user);
            itemName = product.getName();
            itemImage = product.getCoverImage();
            price = nullToZero(product.getPrice());            // 纯积分商品 price=0
            pointsPrice = nullToZero(product.getPointsPrice()); // 纯现金商品 points_price=0
        } else {
            LifeBenefit benefit = benefitService.validateExchange(addDTO.getItemId(), quantity, user);
            itemName = benefit.getName();
            itemImage = benefit.getCoverImage();
            price = 0;                                          // 权益无现金单价, 对齐下单明细快照口径
            pointsPrice = nullToZero(benefit.getPointsPrice());
        }

        // 4. 并发锁(粒度=用户+商品行): 双击/并发请求若同时走到步骤5, 两个都查不到旧行会各插一条,
        //    出现两行同款 → 后续 getOne 命中多行直接报错。setnx 拿不到锁说明同款正在加购, 快速失败
        String lockKey = RedisKeyConstant.CART_ADD_LOCK
                .replace("{userId}", userId.toString())
                .replace("{itemType}", addDTO.getItemType().toString())
                .replace("{itemId}", addDTO.getItemId().toString());
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", RedisKeyConstant.CART_ADD_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new ValidationException("操作太频繁, 请稍后重试");
        }
        try {
            // 5. 合并或插入: 查当前用户同类型同款行
            //    getOne 第二参 false = 命中多行不抛异常取第一条(万一历史脏数据有重复行, 保证接口不炸)
            LifeCart exist = getOne(new LambdaQueryWrapper<LifeCart>()
                    .eq(LifeCart::getUserId, userId)
                    .eq(LifeCart::getItemType, addDTO.getItemType())
                    .eq(LifeCart::getItemId, addDTO.getItemId()), false);
            if (exist != null) {
                int merged = exist.getQuantity() + quantity;
                if (merged > ROW_MAX_QUANTITY) {
                    throw new ValidationException("单个商品数量不能超过" + ROW_MAX_QUANTITY);
                }
                exist.setQuantity(merged);
                // 快照顺带刷成最新值: 商品可能改过名/价/图, 保持购物车行与商品同源
                exist.setItemName(itemName);
                exist.setItemImage(itemImage);
                exist.setPrice(price);
                exist.setPointsPrice(pointsPrice);
                updateById(exist);   // updateTime/updater 由 MyBatisPlusAutoFillHandler 自动填充
            } else {
                LifeCart cart = LifeCart.builder()
                        .userId(userId)
                        .itemType(addDTO.getItemType())
                        .itemId(addDTO.getItemId())
                        .itemName(itemName)
                        .itemImage(itemImage)
                        .price(price)
                        .pointsPrice(pointsPrice)
                        .quantity(quantity)
                        .build();
                save(cart);          // createTime/creater/deleted=0 自动填充, 无需手动 set
            }
        } finally {
            // 6. 释放锁。极端情况: 业务执行超过TTL → 锁已自动过期被他人持有, 此处 delete 会误删他人锁。
            //    加购是毫秒级操作, 5s TTL 下风险可忽略; 严格版做法 = value 存请求ID + Lua 校验后删
            //    (后续做限兑锁 exchange:{userId}:{benefitId} 时建议直接上严格版, 两处同款)
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public List<CartVO> queryCartList() {
        // 1. 查询所有 未删除的 购物车清单
        LambdaQueryWrapper<LifeCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LifeCart::getUserId,BaseContext.getCurrentId())
                .orderByDesc(LifeCart::getCreateTime);
        List<LifeCart> carts = cartMapper.selectList(wrapper);
        if (CollUtil.isEmpty(carts)) {
            return Collections.emptyList();
        }

        // 2. 根据itemtype进行区分
        Map<Integer, List<LifeCart>> typeGroup = carts
                .stream()
                .collect(Collectors.groupingBy(LifeCart::getItemType));
        // 2.1 准备两组map过滤
        Map<Long,LifeProduct> productMap = new HashMap<>();
        Map<Long,LifeBenefit> benefitMap = new HashMap<>();

        if (typeGroup.containsKey(1)) {
            // 2.2 实物商品
            // 根据type获取id集合
            List<Long> productIds = typeGroup.get(1)
                    .stream()
                    .map(LifeCart::getItemId)
                    .distinct()
                    .toList();
            // 查出所有 商品 并转化为 map
            List<LifeProduct> products = productMapper.selectList(
                    new LambdaQueryWrapper<LifeProduct>().in(LifeProduct::getId, productIds)
            );
            productMap = products.stream().collect(
                    // Function.identity(): 对流中每个元素，决定它作为 Map 的 value。Function.identity() 等价于 x -> x，即元素本身
                    // (o1, o2) -> o1 —— 冲突合并函数: 当 出现相同 key 时，用这个函数决定保留哪个值。
                    Collectors.toMap(LifeProduct::getId, Function.identity(),(o1,o2) -> o1)
            );
        }
        if (typeGroup.containsKey(2)) {
            // 2.3 权益商品
            List<Long> benefitIds = typeGroup.get(2)
                    .stream()
                    .map(LifeCart::getItemId)
                    .distinct()
                    .toList();
            List<LifeBenefit> benefits = benefitMapper.selectList(
                    new LambdaQueryWrapper<LifeBenefit>().in(LifeBenefit::getId, benefitIds)
            );
            benefitMap = benefits.stream().collect(
                    Collectors.toMap(LifeBenefit::getId,(x -> x),(o1,o2) -> o1)
            );
        }
        // 3. 组装VO
        List<CartVO> cartVOS = new ArrayList<>();
        for (LifeCart c : carts) {
            CartVO vo = BeanUtils.copyBean(c, CartVO.class);
            if (c.getItemType() == 1) {
                // 实物商品
                LifeProduct product = productMap.get(c.getItemId());
                if (product == null) {
                    vo.setStock(null);
                    vo.setOnSale(false);
                    vo.setValid(false);
                } else {
                    Boolean isOnSale = product.getStatus() != 0;
                    int stock = product.getStock() == null ? -1 : product.getStock();

                    vo.setStock(stock);
                    vo.setCreateTime(LocalDateTime.now());
                    vo.setOnSale(isOnSale);
                    vo.setValid(isOnSale && stock != ProductConstant.NO_STOCK);
                }
            } else {
              // 权益商品
                // 实物商品
                LifeBenefit benefit = benefitMap.get(c.getItemId());
                if (benefit == null) {
                    vo.setStock(null);
                    vo.setOnSale(false);
                    vo.setValid(false);
                } else {
                    Boolean isOnSale = benefit.getStatus() != 0;
                    int stock = benefit.getStock() == null ? -1 : benefit.getStock();
                    // 固定截止权益已过期 = 失效
                    boolean expired = benefit.getValidType() != null
                            && benefit.getValidType() == BenefitConstant.VALID_FIXED
                            && benefit.getFixedEndTime() != null
                            && !benefit.getFixedEndTime().isAfter(LocalDateTime.now());

                    vo.setStock(stock);
                    vo.setCreateTime(LocalDateTime.now());
                    vo.setOnSale(isOnSale && !expired);
                    vo.setValid(isOnSale && !expired && stock != BenefitConstant.NO_STOCK);
                }
            }
            cartVOS.add(vo);
        }

        return cartVOS;
        // TODO 4. 可选: 快照价与实时价不一致时以实时价为准并加 priceChanged 标记提示前端
    }

    @Override
    public void updateQuantity(Long id, Integer quantity) {
        // 1. 参数校验: quantity 是"改后的总量"(绝对值非增量), 上限口径与加购一致, 防止绕过 ROW_MAX_QUANTITY
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("商品数量不正确");
        }
        if (quantity > ROW_MAX_QUANTITY) {
            throw new ValidationException("单个商品数量不能超过" + ROW_MAX_QUANTITY);
        }

        // 2. 查询 当前用户下的购物车，如果查不到 或者 非当前用户一律返回不存在
        Long userId = BaseContext.getCurrentId();
        LifeCart cart = getOne(new LambdaQueryWrapper<LifeCart>()
                .eq(LifeCart::getId, id)
                .eq(LifeCart::getUserId, userId), false);
        if (cart == null) {
            throw new ValidationException("购物车不存在");
        }

        // 3. 用户校验
        LifeUser user = userService.getById(userId);
        if (user == null || !StatusConstant.ENABLE.equals(user.getStatus())) {
            throw new ValidationException("用户不存在或已禁用");
        }

        // 4. 实时校验: 直接复用加购同款公共方法, 零手写库存/过期/限购逻辑 ——
        //    validatePurchase = 存在/上架/库存(-1不限量跳过)/等级/限购;
        //    validateExchange 另含固定截止过期拦截; 商品被删除时由二者的"不存在或已下架"口径兜住,
        //    即失效行只允许删除, 不允许改数量(前端置灰行本就不该发出此请求, 这里防直调)
        if (cart.getItemType() == 1) {
            productService.validatePurchase(cart.getItemId(), quantity, user);
        } else {
            benefitService.validateExchange(cart.getItemId(), quantity, user);
        }

        // 5. 带归属双条件更新, 单语句原子完成: 步骤2到此处若行被并发删除, 0行受影响 → 统一报"不存在";
        //    wrapper 式 update 不走 MetaObjectHandler(updater 不填充), update_time 由 DDL 的
        //    ON UPDATE CURRENT_TIMESTAMP 兜底, 购物车行可接受
        boolean updated = update(new LambdaUpdateWrapper<LifeCart>()
                .eq(LifeCart::getId, id)
                .eq(LifeCart::getUserId, userId)
                .set(LifeCart::getQuantity, quantity));
        if (!updated) {
            throw new ValidationException("购物车不存在");
        }
    }

    @Override
    public void deleteCart(Long id) {
        // 归属校验 + 逻辑删一条语句原子完成: @TableLogic 使 remove 转为 UPDATE deleted=1,
        // WHERE id + user_id 双条件, 0行受影响 = 不存在/非本人/已删过, 统一报"购物车行不存在"(防探测);
        // 重复删除第二次自然报"不存在", 与 PostServiceImpl.deletePostById 同模式
        boolean removed = remove(new LambdaQueryWrapper<LifeCart>()
                .eq(LifeCart::getId, id)
                .eq(LifeCart::getUserId, BaseContext.getCurrentId()));
        if (!removed) {
            throw new ValidationException("购物车不存在");
        }
    }

    @Override
    public void clearCart() {
        // 按当前用户逻辑删全部行; 空车/重复点击清空时 0 行受影响也返回成功(幂等), 故不检查返回值
        remove(new LambdaQueryWrapper<LifeCart>()
                .eq(LifeCart::getUserId, BaseContext.getCurrentId()));
    }
}

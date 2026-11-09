package com.life.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.BalanceTypeConstant;
import com.life.constant.MessageConstant;
import com.life.constant.PayConstant;
import com.life.constant.PointsTypeConstant;
import com.life.constant.RedisConstant;
import com.life.constant.StatusConstant;
import com.life.context.BaseContext;
import com.life.dto.OrderItemDTO;
import com.life.dto.OrderSubmitDTO;
import com.life.dto.PageDTO;
import com.life.entity.*;
import com.life.exception.ValidationException;
import com.life.mapper.*;
import com.life.query.OrderQueryDTO;
import com.life.result.PageResult;
import com.life.service.LifeBenefitService;
import com.life.service.LifeProductService;
import com.life.service.OrderService;
import com.life.utils.BeanUtils;
import com.life.vo.OrderDetailVO;
import com.life.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<LifeOrderMapper, LifeOrder> implements OrderService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private LifeProductMapper productMapper;
    @Autowired
    private LifeOrderDetailMapper orderDetailMapper;
    @Autowired
    private LifeUserAddressMapper userAddressMapper;
    @Autowired
    private LifePointsRecordMapper pointsRecordMapper;
    @Autowired
    private LifeBalanceRecordMapper balanceRecordMapper;
    @Autowired
    private LifePayOrderMapper payOrderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private LifeProductService productService;
    @Autowired
    private LifeBenefitService benefitService;
    @Autowired
    private LifeCartMapper cartMapper;
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 积分抵扣配置(2.5): rate=多少积分抵1元(100积分=1元=100分, 即1积分=1分);
     * maxRatio=抵扣上限占订单金额百分比
     */
    @Value("${life.points.deduct.rate:100}")
    private int deductRate;
    @Value("${life.points.deduct.max-ratio:50}")
    private int deductMaxRatio;

    /**
     * 发货后自动确认收货天数(2.5 配套): 定时任务扫 status=2 且 update_time 超 N 天置3
     */
    @Value("${life.order.auto-confirm-days:7}")
    private int autoConfirmDays;

    /**
     * 签发下单幂等令牌
     * set key init 30min
     */
    @Override
    public String generateOrderToken() {
        Long userId = BaseContext.getCurrentId();
        String token = UUID.randomUUID().toString().replace("-", "");
        redissonClient.<String>getBucket(orderTokenKey(userId, token), StringCodec.INSTANCE)
                .set(RedisConstant.ORDER_TOKEN_INIT, Duration.ofMinutes(RedisConstant.ORDER_TOKEN_TTL_MINUTES));
        return token;
    }

    private String orderTokenKey(Long userId, String token) {
        return RedisConstant.ORDER_TOKEN
                .replace("{userId}", userId.toString())
                .replace("{token}", token.trim());
    }

    /**
     * 消费幂等令牌
     */
    private RBucket<String> consumeOrderToken(Long userId, String token) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("缺少下单令牌, 请重新进入结算页");
        }
        String key = orderTokenKey(userId, token);
        // 获取指定 key 对应的桶。关键点在于第二个参数 StringCodec.INSTANCE。它指定了序列化/反序列化编解码器为字符串编解码器。将对象存入和读取的值，当作纯字符串来处理
        // 避免了 Redisson 默认使用其他编解码器（如 Kryo）可能带来的乱码或反序列化错误。这在处理简单的字符串缓存或计数器时非常有用。
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);

        // compareAndSet(expected, newValue) 是 CAS（Compare-And-Set） 操作
        // 仅当当前值等于 expected 时，才原子地把它改成 newValue，返回 true；否则不做任何修改，返回 false。
        if (!bucket.compareAndSet(RedisConstant.ORDER_TOKEN_INIT, RedisConstant.ORDER_TOKEN_USED)) {
            log.warn("下单令牌流转失败 userId={} key={}", userId, key);
            throw new ValidationException("请勿重复提交订单或令牌已过期, 请重新进入结算页");
        }
        return bucket;
    }

    /**
     * 提交订单。
     */
    @Override
    public OrderVO submitOrder(OrderSubmitDTO submitDTO) {
        Long userId = BaseContext.getCurrentId();
        // 闸1: 令牌 INIT->USED
        RBucket<String> tokenBucket = consumeOrderToken(userId, submitDTO.getOrderToken());
        try {
            OrderVO orderVO = submitWithOrderLocks(userId, submitDTO);
            // 闸2完成: 下单成功, 令牌永久失效; Redis 抖动只留 USED 残留, 后果是多拒一次重放, 方向安全
            settleTokenSuccess(tokenBucket);
            return orderVO;
        } catch (RuntimeException e) {
            // 加锁失败/业务校验/事务回滚: 还原 INIT, 同令牌可重试(极端场景: 提交成功但响应丢失, 由对账兜底)
            restoreTokenForRetry(tokenBucket);
            throw e;
        }
    }

    /**
     * 闸2: 逐 key 获取 RLock(不传 leaseTime=看门狗续期), 全部拿到才进事务;
     * 失败即抛, finally 逆序释放并做持有者校验——极端情况(Redis 分区看门狗停摆、锁已过期中转他人)
     * 直接 unlock 会抛 IllegalMonitorStateException 且误伤他人锁;
     * 不用 MultiLock: 其 isHeldByCurrentThread 等方法直接抛 UnsupportedOperationException, 无法安全守卫
     */
    private OrderVO submitWithOrderLocks(Long userId, OrderSubmitDTO submitDTO) {
        List<String> lockKeys = buildOrderLockKeys(userId, submitDTO.getItems());
        // 无有效订单项可锁时直接进事务, 参数合法性由 doSubmitOrder 统一校验
        if (lockKeys.isEmpty()) {
            return transactionTemplate.execute(status -> doSubmitOrder(submitDTO));
        }
        List<RLock> acquired = new ArrayList<>(lockKeys.size());
        try {
            for (String key : lockKeys) {
                RLock lock = redissonClient.getLock(key);
                long begin = System.currentTimeMillis();
                boolean locked;
                try {
                    // waitTime=0 不重试(拿不齐快速失败, 用户重试即可), leaseTime 缺省走看门狗
                    locked = lock.tryLock(0, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ValidationException("系统繁忙, 请稍后重试");
                }
                long cost = System.currentTimeMillis() - begin;
                if (!locked) {
                    log.warn("下单加锁失败 userId={} key={} cost={}ms", userId, key, cost);
                    throw new ValidationException("商品正在被抢购, 请稍后重试");
                }
                acquired.add(lock);
                if (log.isDebugEnabled()) {
                    log.debug("下单加锁成功 userId={} key={} cost={}ms", userId, key, cost);
                }
            }
            return transactionTemplate.execute(status -> doSubmitOrder(submitDTO));
        } finally {
            for (int i = acquired.size() - 1; i >= 0; i--) {
                RLock lock = acquired.get(i);
                // 检查当前线程是否持有指定的分布式锁 lock，它返回一个布尔值。
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                } else {
                    log.warn("下单锁已非本线程持有(疑看门狗续期失败/锁过期), 跳过释放 key={}", lock.getName());
                }
            }
        }
    }

    private void settleTokenSuccess(RBucket<String> bucket) {
        try {
            bucket.delete();
        } catch (Exception e) {
            log.warn("下单令牌置失效失败(USED残留仅拒绝重放, 不影响订单结果)", e);
        }
    }

    private void restoreTokenForRetry(RBucket<String> bucket) {
        try {
            // 重置完整 TTL: 本请求未产生任何已提交副作用(事务回滚), 还原即允许同令牌重试
            bucket.set(RedisConstant.ORDER_TOKEN_INIT, Duration.ofMinutes(RedisConstant.ORDER_TOKEN_TTL_MINUTES));
        } catch (Exception e) {
            log.warn("下单令牌还原失败, 用户需重新获取令牌", e);
        }
    }

    /**
     * 由订单行推导锁 key: 权益→exchange:lock, 商品→purchase:lock; 非法入参交由事务内统一校验
     * <p>TreeSet 天然完成 去重+字典序排序: 所有并发请求加锁顺序一致, 顺序加锁方案(方案B)无循环等待前提
     */
    private List<String> buildOrderLockKeys(Long userId, List<OrderItemDTO> items) {
        if (userId == null || items == null) {
            return List.of();
        }
        Set<String> keys = new TreeSet<>();
        for (OrderItemDTO item : items) {
            if (item == null || item.getItemId() == null) {
                continue;
            }
            if (item.getItemType() != null && item.getItemType() == 2) {
                keys.add(RedisConstant.EXCHANGE_LOCK
                        .replace("{userId}", userId.toString())
                        .replace("{benefitId}", item.getItemId().toString()));
            } else if (item.getItemType() != null && item.getItemType() == 1) {
                keys.add(RedisConstant.PURCHASE_LOCK
                        .replace("{userId}", userId.toString())
                        .replace("{productId}", item.getItemId().toString()));
            }
        }
        return new ArrayList<>(keys);
    }

    private OrderVO doSubmitOrder(OrderSubmitDTO submitDTO) {
        // 1、基础校验
        List<OrderItemDTO> items = submitDTO.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("订单缺少商品");
        }
        // 2、按商品类型分流并合并数量 map<itemId, 数量>
        Map<Long, Integer> productQtyMap = new LinkedHashMap<>();
        Map<Long, Integer> benefitQtyMap = new LinkedHashMap<>();
        for (OrderItemDTO item : items) {
            if (item.getItemType() == null || (item.getItemType() != 1 && item.getItemType() != 2)) {
                throw new IllegalArgumentException("产品类型不合法");
            }
            int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
            if (quantity <= 0) {
                throw new IllegalArgumentException("商品数量不正确");
            }
            // 判断是实物商品还是权益商品
            Map<Long, Integer> qtyMap = item.getItemType() == 1 ? productQtyMap : benefitQtyMap;
            qtyMap.put(item.getItemId(), qtyMap.getOrDefault(item.getItemId(), 0) + quantity);
        }
        // 订单类型由商品类型推导, 混单会导致orderType无法确定
        if (!productQtyMap.isEmpty() && !benefitQtyMap.isEmpty()) {
            throw new IllegalArgumentException("实物商品与权益不能混合下单");
        }

        // 3、全部先校验并攒明细, 一件不过整单不成立
        String orderNo = generateOrderNumber();
        Long userId = BaseContext.getCurrentId();
        // 两通道共用的用户有效性校验(等级校验依赖user, 在各通道内进行)
        LifeUser user = userMapper.selectById(userId);
        if (user == null || !StatusConstant.ENABLE.equals(user.getStatus())) {
            throw new ValidationException("用户不存在或已禁用");
        }
        List<LifeOrderDetail> details = new ArrayList<>();
        // 校验通过的权益留存, 步骤5扣库存/发券直接复用, 避免二次查询
        List<LifeBenefit> benefits = new ArrayList<>();
        int totalAmount = 0;
        int pointsUsed = 0;
        int orderType;
        if (!productQtyMap.isEmpty()) {
            // 3.1 实物商品订单
            orderType = 1;
            for (Map.Entry<Long, Integer> entry : productQtyMap.entrySet()) {
                // 存在/上架/库存/等级/限购校验复用商品服务(与权益通道同一异常口径), -1表示不限量;
                // 限购并发竞态由 submitOrder 外层 purchase:lock 封死(2.7)
                LifeProduct product = productService.validatePurchase(entry.getKey(), entry.getValue(), user);
                // 商品单以现金计价; points_price 仅等额展示, 不作强制第二货币(可选抵扣见下方 pointsDeduct)
                totalAmount += (product.getPrice() == null ? 0 : product.getPrice()) * entry.getValue();
                // 封装订单详情
                LifeOrderDetail detail = new LifeOrderDetail();
                detail.setOrderNo(orderNo);
                detail.setItemType(1);
                detail.setItemId(product.getId());
                detail.setItemName(product.getName());
                detail.setItemImage(product.getCoverImage());
                detail.setPrice(product.getPrice() == null ? 0 : product.getPrice());
                detail.setQuantity(entry.getValue());
                detail.setTotalAmount((product.getPrice() == null ? 0 : product.getPrice()) * entry.getValue());
                detail.setPointsPrice(product.getPointsPrice());

                details.add(detail);
            }
        } else {
            // 3.2 权益订单(纯积分兑换): 校验与发放复用权益服务公共方法, 下单即兑换唯一入口;
            // 限兑并发竞态由 submitOrder 外层 exchange:lock 封死(2.7)
            orderType = 2;
            for (Map.Entry<Long, Integer> entry : benefitQtyMap.entrySet()) {
                // 校验存在/上架/等级/限兑/库存/有效期, 返回可直接用于快照的权益
                LifeBenefit benefit = benefitService.validateExchange(entry.getKey(), entry.getValue(), user);
                benefits.add(benefit);
                pointsUsed += benefit.getPointsPrice() * entry.getValue();

                // 权益无现金单价, price/totalAmount 落0, 积分价进pointsPrice快照
                LifeOrderDetail detail = new LifeOrderDetail();
                detail.setOrderNo(orderNo);
                detail.setItemType(2);
                detail.setItemId(benefit.getId());
                detail.setItemName(benefit.getName());
                detail.setItemImage(benefit.getCoverImage());
                detail.setPrice(0);
                detail.setQuantity(entry.getValue());
                detail.setTotalAmount(0);
                detail.setPointsPrice(benefit.getPointsPrice());

                details.add(detail);
            }
        }
        // 3.3 商品订单可选积分抵扣(2.5): 用积分抵现金, 需为 rate 整数倍且不超过金额 maxRatio%
        int pointsDeduct = 0;
        int deductFen = 0;
        if (orderType == 1 && submitDTO.getPointsDeduct() != null && submitDTO.getPointsDeduct() > 0) {
            pointsDeduct = submitDTO.getPointsDeduct();
            if (deductRate <= 0 || pointsDeduct % deductRate != 0) {
                throw new ValidationException("积分抵扣需为" + deductRate + "的整数倍(整元)");
            }
            deductFen = pointsDeduct / deductRate * 100;
            int maxFen = (int) (totalAmount * deductMaxRatio / 100.0);
            if (deductFen > maxFen) {
                throw new ValidationException("积分抵扣最多不超过订单金额的" + deductMaxRatio + "%");
            }
        }
        // 4、订单头: 权益订单纯积分即时结算(status=1), 商品订单待支付(status=0)
        LifeOrder order = new LifeOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setOrderType(orderType);
        order.setStatus(orderType == 2 ? 1 : 0);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(orderType == 2 ? 0 : totalAmount - deductFen);
        order.setPointsUsed(pointsUsed + pointsDeduct);   // 权益单=兑换积分; 商品单=抵扣积分(可选)
        order.setRemark(submitDTO.getRemark());
        // 实物订单: 校验地址归属当前用户, 并以JSON快照落库(地址后续修改不影响历史订单)
        if (orderType == 1) {
            if (submitDTO.getAddressId() == null) {
                throw new IllegalArgumentException("实物订单需要选择收货地址");
            }
            LifeUserAddress address = userAddressMapper.selectById(submitDTO.getAddressId());
            if (address == null || !address.getUserId().equals(userId)) {
                throw new ValidationException(MessageConstant.ADDRESS_BOOK_IS_NULL);
            }
            order.setDeliveryAddress(JSON.toJSONString(address));
        }
        baseMapper.insert(order);

        // 5、明细回填orderId后插入
        for (LifeOrderDetail detail : details) {
            detail.setOrderId(order.getId());
            orderDetailMapper.insert(detail);
        }

        if (orderType == 1) {
            // 5.1 商品订单积分抵扣(可选, 2.5): 原子扣减 0行=积分不足整体回滚, CONSUME 流水留痕;
            //     (pointsUsed 变量对商品单恒为0, 保留加法为纯积分商品预留通道, 随 2.8 冻结模型启用)
            int productPoints = pointsUsed + pointsDeduct;
            if (productPoints > 0) {
                if (userMapper.deductPoints(userId, productPoints) == 0) {
                    throw new ValidationException(MessageConstant.USER_POINTS_NOT_ENOUGH);
                }
                pointsRecordMapper.insert(LifePointsRecord.builder()
                        .userId(userId)
                        .type(PointsTypeConstant.CONSUME)
                        .points(-productPoints)
                        .orderNo(orderNo)
                        .remark("商品订单积分抵扣")
                        .build());
            }
            // 5.2 原子扣库存, 0行=被并发抢完, 抛异常触发整体回滚
            for (LifeOrderDetail detail : details) {
                if (productMapper.deductStock(detail.getItemId(), detail.getQuantity()) == 0) {
                    throw new IllegalArgumentException("商品已被抢完: " + detail.getItemName());
                }
            }
        } else {
            // 权益订单交易链: 扣积分 -> 积分流水 -> 逐权益扣库存并发放券码(挂真实订单号可追溯)
            if (userMapper.deductPoints(userId, pointsUsed) == 0) {
                throw new ValidationException(MessageConstant.USER_POINTS_NOT_ENOUGH);
            }
            pointsRecordMapper.insert(LifePointsRecord.builder()
                    .userId(userId)
                    .type(PointsTypeConstant.EXCHANGE)
                    .points(-pointsUsed)
                    .orderNo(orderNo)
                    .build());
            for (LifeBenefit benefit : benefits) {
                // 原子扣库存+销量(0行=并发抢完), 按购买数量逐张发放
                benefitService.grantExchange(benefit, benefitQtyMap.get(benefit.getId()), userId, orderNo);
            }
        }
        // 6、购物车来源单: 同事务清理已下单的购物车行(直购路径不传 cartIds, 跳过)
        //    userId 归属条件兜底: 混入他人/不存在的 id 只是静默不命中, 不报错也无探测泄露;
        //    清理与下单同在本事务, 下单失败整体回滚、购物车行保留可直接重试, 成功则必清、不残留已购商品
        if (submitDTO.getCartIds() != null && !submitDTO.getCartIds().isEmpty()) {
            cartMapper.delete(new LambdaQueryWrapper<LifeCart>()
                    .in(LifeCart::getId, submitDTO.getCartIds())
                    .eq(LifeCart::getUserId, userId));
        }
        // 7、组装VO返回
        OrderVO orderVO = BeanUtils.copyBean(order, OrderVO.class);
        orderVO.setDetails(BeanUtils.copyList(details, OrderDetailVO.class));
        return orderVO;
    }

    @Override
    public PageDTO<OrderVO> queryMyOrderPage(OrderQueryDTO query) {
        // 获取用户id
        Long userId = BaseContext.getCurrentId();
        // 分页查询 查询出当前用户下的订单, 按照时间排序
        LambdaQueryWrapper<LifeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LifeOrder::getUserId, userId);
        // 订单类型筛选，不传就是查全部订单 1-商品订单 2-权益订单
        if (query.getOrderType() != null) {
            wrapper.eq(LifeOrder::getOrderType, query.getOrderType());
        }
        // 订单状态筛选
        if (query.getStatus() != null) {
            wrapper.eq(LifeOrder::getStatus, query.getStatus());
        }
        // 按下单时间降序
        wrapper.orderByDesc(LifeOrder::getCreateTime);
        Page<LifeOrder> page = this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        // 转化为vo,分页查询列表时不需要查明细, 只需要订单头
        List<LifeOrder> records = page.getRecords();
        List<OrderVO> orderVOs = new ArrayList<>();
        for (LifeOrder r : records) {
            OrderVO orderVO = BeanUtils.copyBean(r, OrderVO.class);
            orderVO.setDetails(null);
            orderVOs.add(orderVO);
        }

        return new PageDTO<>(page.getTotal(), page.getPages(), orderVOs);
    }

    @Override
    public OrderVO getOrderDetail(Long id) {
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<LifeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LifeOrder::getId, id)
                .eq(LifeOrder::getUserId, userId);
        LifeOrder order = this.getOne(wrapper);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        OrderVO orderVO = BeanUtils.copyBean(order, OrderVO.class);
        orderVO.setDetails(BeanUtils.copyList(orderDetailMapper
                .selectList(new LambdaQueryWrapper<LifeOrderDetail>()
                        .eq(LifeOrderDetail::getOrderId, id)), OrderDetailVO.class));
        return orderVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id) {
        // 查询当前用户下的订单是否存在, 且状态为待支付
        LambdaQueryWrapper<LifeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LifeOrder::getId, id)
                .eq(LifeOrder::getUserId, BaseContext.getCurrentId());
        LifeOrder order = this.getOne(wrapper);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        // 权益订单为纯积分即时成交且券码已发放, 不支持自助取消, 退款走管理端作废
        if (order.getOrderType() == 2) {
            throw new IllegalArgumentException("权益订单不支持取消, 如有需要请联系客服作废退回");
        }
        if (order.getStatus() != 0) {
            throw new IllegalArgumentException("订单未支付才可以取消订单");
        }
        order.setStatus(4);
        baseMapper.updateById(order);
        // 下单时已扣库存, 取消需退回; 置状态与退库存同事务, 要么全成要么全回滚
        List<LifeOrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<LifeOrderDetail>()
                .eq(LifeOrderDetail::getOrderId, id));
        for (LifeOrderDetail detail : details) {
            productMapper.backStock(detail.getItemId(), detail.getQuantity());
        }
        // 2.5 积分回滚: 商品单支付/抵扣过的积分原路退回(REFUND 流水, 与退库存同事务)
        if (order.getPointsUsed() != null && order.getPointsUsed() > 0) {
            userMapper.addPoints(order.getUserId(), order.getPointsUsed());
            pointsRecordMapper.insert(LifePointsRecord.builder()
                    .userId(order.getUserId())
                    .type(PointsTypeConstant.REFUND)
                    .points(order.getPointsUsed())
                    .orderNo(order.getOrderNo())
                    .remark("订单取消退回积分")
                    .build());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long id) {
        Long userId = BaseContext.getCurrentId();
        LifeOrder order = getById(id);
        // 不存在/非本人统一报不存在, 防探测
        if (order == null || !order.getUserId().equals(userId)) {
            throw new ValidationException("订单不存在");
        }
        // 仅待支付可支付(权益单纯积分即时成交status直接为1, 天然无支付环节)
        if (order.getStatus() != 0) {
            throw new ValidationException("订单当前状态不可支付");
        }

        // 1. 落支付单: 模拟支付同步成功直接置2, 真实渠道接入后改为 0->回调->2; uk_biz_order_no 保证与业务单1:1
        LifePayOrder payOrder = LifePayOrder.builder()
                .bizOrderNo(order.getOrderNo())
                .payOrderNo("PAY" + order.getOrderNo())
                .totalAmount(order.getPayAmount())
                .pointsAmount(order.getPointsUsed())
                .status(PayConstant.PAY_SUCCESS)
                .channelCode(PayConstant.CHANNEL_BALANCE)
                .payTime(LocalDateTime.now())
                // 第三方交易号占位, 真实渠道接入后回填
                .tradeNo("MOCK" + System.currentTimeMillis())
                .build();
        payOrderMapper.insert(payOrder);

        // 2. 原子扣余额(条件 money >= n 同行判定, 0行=余额不足, 整体回滚)
        if (userMapper.deductMoney(userId, order.getPayAmount()) == 0) {
            throw new ValidationException(MessageConstant.USER_MONEY_NOT_ENOUGH);
        }

        // 3. 余额流水
        balanceRecordMapper.insert(LifeBalanceRecord.builder()
                .userId(userId)
                .amount(-order.getPayAmount())
                .type(BalanceTypeConstant.CONSUME)
                .orderNo(order.getOrderNo())
                .remark("订单支付: " + order.getOrderNo())
                .build());

        // 4. 业务单置已支付
        order.setStatus(1);
        order.setPayChannel(PayConstant.CHANNEL_BALANCE);
        order.setPayTime(payOrder.getPayTime());
        updateById(order);
    }

    @Override
    public PageResult queryAdminOrderPage(OrderQueryDTO query) {
        // 管理端全量订单: 可按状态/类型/用户/下单时间区间筛选, 时间降序
        LambdaQueryWrapper<LifeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getUserId() != null, LifeOrder::getUserId, query.getUserId())
                .eq(query.getOrderType() != null, LifeOrder::getOrderType, query.getOrderType())
                .eq(query.getStatus() != null, LifeOrder::getStatus, query.getStatus())
                .ge(query.getStartTime() != null, LifeOrder::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, LifeOrder::getCreateTime, query.getEndTime())
                .orderByDesc(LifeOrder::getCreateTime);
        Page<LifeOrder> page = this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<OrderVO> orderVOs = BeanUtils.copyList(page.getRecords(), OrderVO.class);
        // 关联下单人昵称: 批量查询一次, 避免逐行查
        if (!orderVOs.isEmpty()) {
            Set<Long> userIds = orderVOs.stream().map(OrderVO::getUserId).collect(Collectors.toSet());
            Map<Long, String> nameMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(LifeUser::getId,
                            u -> u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername()));
            orderVOs.forEach(vo -> vo.setNickname(nameMap.get(vo.getUserId())));
        }
        return new PageResult(page.getTotal(), orderVOs);
    }

    @Override
    public void deliverOrder(Long id) {
        // 根据订单id查询订单
        LifeOrder order = getById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new IllegalArgumentException("订单已支付才可以发货");
        }
        order.setStatus(2);
        baseMapper.updateById(order);
    }

    @Override
    public void confirmOrder(Long id) {
        Long userId = BaseContext.getCurrentId();
        LifeOrder order = getById(id);
        // 不存在/非本人统一报不存在
        if (order == null || !order.getUserId().equals(userId)) {
            throw new ValidationException("订单不存在");
        }
        // 仅已发货可确认收货: 状态 2→3, 补齐商品单状态机最后一个断点
        // (权益单提交即置1, 永远到不了2, 天然被此校验拦截, 不会误确认)
        if (order.getStatus() != 2) {
            throw new ValidationException("订单未发货, 不可确认收货");
        }
        order.setStatus(3);
        updateById(order);
        // 超时未确认的兜底由 OrderAutoConfirmTask 定时批量置3(配置 life.order.auto-confirm-days)
    }

    /**
     * 超时自动确认收货(定时任务入口, 无登录态): status=2 且 update_time 早于 N 天前 → 批量置3。
     * 发货时间无独立字段, 以 update_time 近似(发货置2后若无其他更新即等价);
     * 二期随 2.15 加 deliver_time 列精确化
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoConfirmTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(autoConfirmDays);
        LambdaUpdateWrapper<LifeOrder> wrapper = new LambdaUpdateWrapper<LifeOrder>()
                .eq(LifeOrder::getStatus, 2)
                .lt(LifeOrder::getUpdateTime, deadline)
                .set(LifeOrder::getStatus, 3)
                .set(LifeOrder::getUpdateTime, LocalDateTime.now());
        return baseMapper.update(null, wrapper);
    }

    private String generateOrderNumber() {
        String code = "LP";
        // 时间戳
        String time = DateUtil.now().substring(0, 10).replace("-", "");

        // 后缀使用redis自增
        String key = "order:count:" + time;
        long incr = redisTemplate.opsForValue().increment(key);
        // INCR对不存在的key返回1, 仅首次需要设TTL
        if (incr == 1) {
            // 设置过期时间，24h + 10s 避免并发导致订单号重复
            redisTemplate.expire(key, 24 * 60 * 60 + 10, TimeUnit.SECONDS);
        }

        String incrStr = String.format("%06d", incr);

        return code + time + incrStr;
    }
}

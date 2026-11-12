package com.life.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.BalanceTypeConstant;
import com.life.constant.MessageConstant;
import com.life.constant.MqConstant;
import com.life.constant.NoticeTypeConstant;
import com.life.constant.PayConstant;
import com.life.constant.PointsRecordStatusConstant;
import com.life.constant.PointsTypeConstant;
import com.life.constant.RedisConstant;
import com.life.constant.StatusConstant;
import com.life.context.BaseContext;
import com.life.dto.MqNoticeMessage;
import com.life.dto.MqOrderCloseMessage;
import com.life.dto.OrderItemDTO;
import com.life.dto.OrderSubmitDTO;
import com.life.dto.PageDTO;
import com.life.entity.*;
import com.life.exception.ValidationException;
import com.life.mapper.*;
import com.life.mq.MqProducer;
import com.life.query.OrderQueryDTO;
import com.life.result.PageResult;
import com.life.service.LifeBenefitService;
import com.life.service.LifeProductService;
import com.life.service.OrderService;
import com.life.service.PointsService;
import com.life.service.RiskService;
import com.life.service.StockCacheService;
import com.life.constant.RiskActionConstant;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    // ===== 2.5/2.8/2.10/2.12/2.14 新增依赖 =====
    @Autowired
    private PointsService pointsService;
    @Autowired
    private StockCacheService stockCacheService;
    @Autowired
    private MqProducer mqProducer;
    @Autowired
    private RiskService riskService;
    @Autowired
    private LifeRefundOrderMapper refundOrderMapper;
    @Autowired
    private LifePayChannelMapper payChannelMapper;
    @Autowired
    private LifeBenefitMapper benefitMapper;
    @Autowired
    private com.life.properties.StockProperties stockProperties;

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
     * 超时未支付自动关单分钟数(2.5): 延迟消息TTL与扫描兜底共用同一时限
     */
    @Value("${life.order.auto-close-minutes:30}")
    private int autoCloseMinutes;

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
        // 2.10 风控闸门: 被标记风险的用户禁止下单(滑动窗口在 checkAndCollect 内计数)
        riskService.checkAndCollect(RiskActionConstant.ORDER, userId);
        // 闸1: 令牌 INIT->USED
        RBucket<String> tokenBucket = consumeOrderToken(userId, submitDTO.getOrderToken());
        try {
            OrderVO orderVO = submitWithOrderLocks(userId, submitDTO);
            // 闸2完成: 下单成功, 令牌永久失效; Redis 抖动只留 USED 残留, 后果是多拒一次重放, 方向安全
            settleTokenSuccess(tokenBucket);
            // 事务已提交, 此处发 MQ 不会产生"消息先于订单可见/订单回滚消息已出"的幽灵事件
            afterSubmitPublished(orderVO);
            return orderVO;
        } catch (RuntimeException e) {
            // 加锁失败/业务校验/事务回滚: 还原 INIT, 同令牌可重试(极端场景: 提交成功但响应丢失, 由对账兜底)
            restoreTokenForRetry(tokenBucket);
            throw e;
        }
    }

    /**
     * 下单成功后的异步事件(2.12): 实物单发 TTL 延迟关单消息(到期死信转发 order.close.queue);
     * 权益单即时成交, 发兑换成功站内通知; 均 sendQuiet —— MQ 故障不阻断下单(关单有扫描兜底, 通知可缺省)
     */
    private void afterSubmitPublished(OrderVO orderVO) {
        if (orderVO == null) {
            return;
        }
        if (orderVO.getOrderType() != null && orderVO.getOrderType() == 1
                && orderVO.getStatus() != null && orderVO.getStatus() == 0) {
            mqProducer.sendDelay(MqConstant.ORDER_DELAY_EXCHANGE, MqConstant.ORDER_DELAY_ROUTING_KEY,
                    MqOrderCloseMessage.builder().orderId(orderVO.getId()).orderNo(orderVO.getOrderNo()).build(),
                    autoCloseMinutes * 60_000L);
        } else if (orderVO.getOrderType() != null && orderVO.getOrderType() == 2) {
            mqProducer.sendQuiet(MqConstant.NOTICE_EXCHANGE, MqConstant.NOTICE_ROUTING_KEY,
                    MqNoticeMessage.builder()
                            .msgId(UUID.randomUUID().toString())
                            .userId(orderVO.getUserId())
                            .type(NoticeTypeConstant.BENEFIT)
                            .title("兑换成功")
                            .content("订单 " + orderVO.getOrderNo() + " 兑换成功, 消耗积分 "
                                    + orderVO.getPointsUsed() + ", 券码已发放至「我的权益」")
                            .bizId(orderVO.getId())
                            .url("/benefit")
                            .build());
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

        // 5、结算资源(积分冻结/扣减 + 库存扣减)。Redis 库存镜像(2.14)非事务资源:
        //    预扣成功后本事务任意失败, catch 内逆序回补镜像, 极端残留由对账任务以DB为准校正
        List<String[]> redisDeducted = new ArrayList<>();
        try {
            if (orderType == 1) {
                // 5.1 商品订单积分抵扣(2.8 冻结模型): 下单 可用→冻结(FROZEN流水),
                //     支付成功 settleFrozen 转扣减, 取消/超时关单 unfreezeFrozen 解冻回可用
                int productPoints = pointsUsed + pointsDeduct;
                if (productPoints > 0) {
                    pointsService.freezePoints(userId, productPoints,
                            "PF:" + orderNo, orderNo, "商品订单积分抵扣冻结");
                }
                // 5.2 库存: Redis+Lua 预扣(热点挡量) → DB 条件扣减(权威兜底), 0行=被并发抢完整体回滚
                for (LifeOrderDetail detail : details) {
                    preDeductRedisStock(StockCacheService.TYPE_PRODUCT,
                            detail.getItemId(), detail.getQuantity(), redisDeducted);
                    if (productMapper.deductStock(detail.getItemId(), detail.getQuantity()) == 0) {
                        throw new IllegalArgumentException("商品已被抢完: " + detail.getItemName());
                    }
                }
            } else {
                // 权益订单交易链: 纯积分即时结算 → 统一入口 changePoints(bizNo=EX:{orderNo} 幂等)
                pointsService.changePoints(userId, -pointsUsed, PointsTypeConstant.EXCHANGE,
                        "EX:" + orderNo, orderNo, null);
                for (LifeBenefit benefit : benefits) {
                    int qty = benefitQtyMap.get(benefit.getId());
                    preDeductRedisStock(StockCacheService.TYPE_BENEFIT, benefit.getId(), qty, redisDeducted);
                    // 原子扣库存+销量后按数量逐张发放券码(挂真实订单号可追溯)
                    benefitService.grantExchange(benefit, qty, userId, orderNo);
                }
            }
        } catch (RuntimeException e) {
            for (int i = redisDeducted.size() - 1; i >= 0; i--) {
                String[] d = redisDeducted.get(i);
                stockCacheService.rollback(d[0], Long.valueOf(d[1]), Integer.parseInt(d[2]));
            }
            throw e;
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
        // CAS 置4: 用户取消/延迟关单消息/扫描兜底 三方并发只有一方成功, 败者直接退出(资源不重复回滚)
        int updated = baseMapper.update(null, new LambdaUpdateWrapper<LifeOrder>()
                .eq(LifeOrder::getId, id)
                .eq(LifeOrder::getStatus, 0)
                .set(LifeOrder::getStatus, 4)
                .set(LifeOrder::getUpdateTime, LocalDateTime.now()));
        if (updated == 0) {
            throw new IllegalArgumentException("订单状态已变化, 请刷新后重试");
        }
        // 下单时已扣库存/冻结积分, 取消需回退; 与置状态同事务, 要么全成要么全回滚
        rollbackOrderResources(order);
    }

    /**
     * 订单资源回滚公共段(取消/超时关单共用):
     * 库存 DB回退 + Redis镜像回补(2.14) ; 抵扣积分 解冻回可用(2.8), 旧口径订单(即时扣减)原路退回
     */
    private void rollbackOrderResources(LifeOrder order) {
        List<LifeOrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<LifeOrderDetail>()
                .eq(LifeOrderDetail::getOrderId, order.getId()));
        for (LifeOrderDetail detail : details) {
            if (detail.getItemType() != null && detail.getItemType() == 2) {
                // 防御分支: 权益单券码已发放, 库存回退属管理端作废链路, 取消/关单不回
                continue;
            }
            productMapper.backStock(detail.getItemId(), detail.getQuantity());
            stockCacheService.rollback(StockCacheService.TYPE_PRODUCT, detail.getItemId(), detail.getQuantity());
        }
        if (order.getPointsUsed() != null && order.getPointsUsed() > 0) {
            boolean unfrozen = pointsService.unfreezeFrozen("PF:" + order.getOrderNo());
            if (!unfrozen) {
                // 兼容 2.8 之前的旧口径订单(下单即扣减, 无冻结流水): 原路退回, bizNo 幂等防双退
                pointsService.changePoints(order.getUserId(), order.getPointsUsed(), PointsTypeConstant.REFUND,
                        "OCRF:" + order.getOrderNo(), order.getOrderNo(), "订单取消退回积分");
            }
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

        // 0. 渠道表驱动校验(2.6): life_pay_channel 有数据时 BALANCE 必须启用; 空表兼容放行
        Long channelTotal = payChannelMapper.selectCount(null);
        if (channelTotal != null && channelTotal > 0) {
            Long balanceEnabled = payChannelMapper.selectCount(new LambdaQueryWrapper<LifePayChannel>()
                    .eq(LifePayChannel::getCode, PayConstant.CHANNEL_BALANCE)
                    .eq(LifePayChannel::getStatus, 1));
            if (balanceEnabled == null || balanceEnabled == 0) {
                throw new ValidationException("余额支付渠道未开放, 请稍后再试");
            }
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

        // 5. 抵扣积分冻结转扣减(2.8): FROZEN→SETTLED; 旧口径订单无冻结流水返回false属正常
        pointsService.settleFrozen("PF:" + order.getOrderNo());

        // 5.1 消费力度返积分(2026-11-12): 基础返(1分/元)+阶梯加赠, 同事务入账(bizNo=REBATE:单号 幂等)
        int rebate = pointsService.computeConsumeRebate(order.getPayAmount() == null ? 0 : order.getPayAmount());
        if (rebate > 0) {
            pointsService.changePoints(userId, rebate, PointsTypeConstant.ACTIVITY,
                    "REBATE:" + order.getOrderNo(), order.getOrderNo(),
                    "消费返积分(实付¥" + String.format("%.2f", (order.getPayAmount() == null ? 0 : order.getPayAmount()) / 100.0) + ")");
        }

        // 6. 支付成功站内通知(2.12): 事务提交后再发, 避免回滚产生幽灵通知
        Long nUserId = userId;
        String nOrderNo = order.getOrderNo();
        Long nOrderId = order.getId();
        Integer nAmount = order.getPayAmount();
        int nRebate = rebate;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendOrderNotice(nUserId, "支付成功", "订单 " + nOrderNo + " 支付成功, 实付 ¥"
                            + String.format("%.2f", (nAmount == null ? 0 : nAmount) / 100.0)
                            + (nRebate > 0 ? ", 已返 " + nRebate + " 积分" : "")
                            + ", 等待商家发货", nOrderId, "/orders");
                }
            });
        }
    }

    /**
     * 订单类站内通知统一投递(MQ异步, sendQuiet不阻断主流程)
     */
    private void sendOrderNotice(Long userId, String title, String content, Long orderId, String url) {
        mqProducer.sendQuiet(MqConstant.NOTICE_EXCHANGE, MqConstant.NOTICE_ROUTING_KEY,
                MqNoticeMessage.builder()
                        .msgId(UUID.randomUUID().toString())
                        .userId(userId)
                        .type(NoticeTypeConstant.ORDER)
                        .title(title)
                        .content(content)
                        .bizId(orderId)
                        .url(url)
                        .build());
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

    /**
     * 2.5 管理端退款(实物单): 退款单 uk_biz_order_no 一单一退 + 订单 CAS 置5 双闸防重复;
     * 余额/积分原路退回(积分走 changePoints bizNo=RF:{orderNo} 幂等) + 库存回退(DB+Redis镜像);
     * 权益兑换单不走本口(券已发放, 按张作废退积分见 LifeUserBenefitService.voidUserBenefit)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long id, String reason) {
        LifeOrder order = getById(id);
        if (order == null) {
            throw new ValidationException("订单不存在");
        }
        if (order.getOrderType() != null && order.getOrderType() == 2) {
            throw new ValidationException("权益兑换单请走管理端权益作废流程(按张退积分)");
        }
        Integer st = order.getStatus();
        if (st == null || (st != 1 && st != 2)) {
            throw new ValidationException("仅已支付或已发货的订单可退款");
        }

        // 1. 退款单落库: 唯一键 uk_biz_order_no 拦截二次退款; 余额渠道同步退成 status=2
        LifeRefundOrder refund = LifeRefundOrder.builder()
                .bizOrderNo(order.getOrderNo())
                .refundNo("RF" + order.getOrderNo())
                .refundAmount(order.getPayAmount() == null ? 0 : order.getPayAmount())
                .status(2)
                .channelCode(order.getPayChannel() == null ? PayConstant.CHANNEL_BALANCE : order.getPayChannel())
                .reason(reason)
                .refundTime(LocalDateTime.now())
                .tradeNo("MOCKRF" + System.currentTimeMillis())
                .build();
        try {
            refundOrderMapper.insert(refund);
        } catch (DuplicateKeyException e) {
            throw new ValidationException("该订单已存在退款记录, 请勿重复操作");
        }

        // 2. 订单 CAS 置5: 与发货/确认收货/重复退款并发时只有一方成功, 失败即整体回滚(退款单一并撤销)
        int upd = baseMapper.update(null, new LambdaUpdateWrapper<LifeOrder>()
                .eq(LifeOrder::getId, id)
                .in(LifeOrder::getStatus, 1, 2)
                .set(LifeOrder::getStatus, 5)
                .set(LifeOrder::getUpdateTime, LocalDateTime.now()));
        if (upd == 0) {
            throw new ValidationException("订单状态已变化, 退款失败");
        }

        // 3. 余额原路退回(实付>0才退) + REFUND 流水
        if (order.getPayAmount() != null && order.getPayAmount() > 0) {
            userMapper.addMoney(order.getUserId(), order.getPayAmount());
            balanceRecordMapper.insert(LifeBalanceRecord.builder()
                    .userId(order.getUserId())
                    .amount(order.getPayAmount())
                    .type(BalanceTypeConstant.REFUND)
                    .orderNo(order.getOrderNo())
                    .remark("管理端退款: " + order.getOrderNo())
                    .build());
        }

        // 4. 积分原路退回(抵扣部分; bizNo=RF:{orderNo} 与取消退回 OCRF: 前缀隔离, 双路径幂等)
        if (order.getPointsUsed() != null && order.getPointsUsed() > 0) {
            pointsService.changePoints(order.getUserId(), order.getPointsUsed(), PointsTypeConstant.REFUND,
                    "RF:" + order.getOrderNo(), order.getOrderNo(), "管理端退款退回积分");
        }

        // 4.1 消费返积分冲回(2026-11-12): 同公式计算冲回额; 取 min(返额, 当前余额) 防余额不足卡退款,
        //     差额豁免(口径同过期回收); bizNo=REBATEC:{orderNo} 与发放 REBATE: 前缀隔离幂等
        int rebate = pointsService.computeConsumeRebate(order.getPayAmount() == null ? 0 : order.getPayAmount());
        if (rebate > 0) {
            LifeUser ru = userMapper.selectById(order.getUserId());
            int balance = ru == null || ru.getPoints() == null ? 0 : ru.getPoints();
            int claw = Math.min(rebate, balance);
            if (claw > 0) {
                pointsService.changePoints(order.getUserId(), -claw, PointsTypeConstant.ACTIVITY,
                        "REBATEC:" + order.getOrderNo(), order.getOrderNo(), "订单退款冲回消费返积分");
            }
        }

        // 5. 库存回退(DB权威 + Redis镜像同步回补)
        List<LifeOrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<LifeOrderDetail>()
                .eq(LifeOrderDetail::getOrderId, id));
        for (LifeOrderDetail detail : details) {
            productMapper.backStock(detail.getItemId(), detail.getQuantity());
            stockCacheService.rollback(StockCacheService.TYPE_PRODUCT, detail.getItemId(), detail.getQuantity());
        }

        // 6. 退款成功通知(事务提交后发)
        Long nUserId = order.getUserId();
        String nOrderNo = order.getOrderNo();
        Integer nAmount = refund.getRefundAmount();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendOrderNotice(nUserId, "退款成功", "订单 " + nOrderNo + " 已退款 ¥"
                            + String.format("%.2f", (nAmount == null ? 0 : nAmount) / 100.0)
                            + ", 积分已原路退回", order.getId(), "/orders");
                }
            });
        }
        log.info("管理端退款完成 orderId={} orderNo={} amount={} reason={}", id, order.getOrderNo(),
                refund.getRefundAmount(), reason);
    }

    /**
     * 2.5/2.12 超时关单幂等入口: CAS status 0→4, 已支付/已取消/已关闭自然0行跳过;
     * 延迟消息消费、扫描兜底任务、(未来)人工关单三方共用, 资源回滚与状态流转同事务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeOrderIfPending(Long orderId) {
        if (orderId == null) {
            return false;
        }
        int upd = baseMapper.update(null, new LambdaUpdateWrapper<LifeOrder>()
                .eq(LifeOrder::getId, orderId)
                .eq(LifeOrder::getStatus, 0)
                .set(LifeOrder::getStatus, 4)
                .set(LifeOrder::getUpdateTime, LocalDateTime.now()));
        if (upd == 0) {
            return false;
        }
        LifeOrder order = getById(orderId);
        if (order != null) {
            rollbackOrderResources(order);
            log.info("超时关单 orderId={} orderNo={}", orderId, order.getOrderNo());
        }
        return true;
    }

    /**
     * 2.5 扫描兜底关单(定时任务入口): 延迟消息丢失/broker宕机期间的超时单由此收敛;
     * 单批200防长事务, 单单独立小事务互不影响
     */
    @Override
    public int closeTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(autoCloseMinutes);
        List<LifeOrder> pending = baseMapper.selectList(new LambdaQueryWrapper<LifeOrder>()
                .eq(LifeOrder::getStatus, 0)
                .eq(LifeOrder::getOrderType, 1)
                .lt(LifeOrder::getCreateTime, deadline)
                .last("LIMIT 200"));
        int closed = 0;
        for (LifeOrder o : pending) {
            try {
                Boolean done = transactionTemplate.execute(s -> closeOrderIfPending(o.getId()));
                if (Boolean.TRUE.equals(done)) {
                    closed++;
                }
            } catch (Exception e) {
                log.warn("超时关单失败 orderId={} (下轮重试)", o.getId(), e);
            }
        }
        if (closed > 0) {
            log.info("超时关单兜底扫描: 本轮关闭 {} 单", closed);
        }
        return closed;
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

    /**
     * 2.14 Redis+Lua 库存预扣: SOLD_OUT 快速失败(不打DB); MISS(未预热)按需预热后重试一次;
     * 扣成功的条目记入补偿列表(事务回滚时逆序回补); 不限量(库存-1/null)不建镜像恒走DB路径
     */
    private void preDeductRedisStock(String itemType, Long itemId, int qty, List<String[]> compensation) {
        long r = stockCacheService.tryDeduct(itemType, itemId, qty);
        if (r == StockCacheService.MISS && stockProperties.isPreheatOnDemand()) {
            Integer dbStock = null;
            if (StockCacheService.TYPE_PRODUCT.equals(itemType)) {
                LifeProduct p = productMapper.selectById(itemId);
                dbStock = p == null ? null : p.getStock();
            } else {
                LifeBenefit b = benefitMapper.selectById(itemId);
                dbStock = b == null ? null : b.getStock();
            }
            if (dbStock != null && dbStock >= 0 && stockCacheService.preheat(itemType, itemId, dbStock)) {
                r = stockCacheService.tryDeduct(itemType, itemId, qty);
            }
        }
        if (r == StockCacheService.SOLD_OUT) {
            // 镜像判定库存不足: 直接失败不打DB(镜像若漂移偏小, 对账任务10分钟级校正; DB扣减仍是权威)
            throw new ValidationException("库存不足, 已被抢完");
        }
        if (r >= 0) {
            compensation.add(new String[]{itemType, String.valueOf(itemId), String.valueOf(qty)});
        }
    }
}

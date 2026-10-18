package com.life.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.BalanceTypeConstant;
import com.life.constant.MessageConstant;
import com.life.constant.PayConstant;
import com.life.constant.PointsTypeConstant;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<LifeOrderMapper, LifeOrder> implements OrderService {

    @Autowired
    private StringRedisTemplate redisTemplate;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO submitOrder(OrderSubmitDTO submitDTO) {
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
                // 存在/上架/库存/等级/限购校验复用商品服务(与权益通道同一异常口径), -1表示不限量
                LifeProduct product = productService.validatePurchase(entry.getKey(), entry.getValue(), user);
                // 计算订单总价
                totalAmount += product.getPrice() * entry.getValue();
                // 封装订单详情
                LifeOrderDetail detail = new LifeOrderDetail();
                detail.setOrderNo(orderNo);
                detail.setItemType(1);
                detail.setItemId(product.getId());
                detail.setItemName(product.getName());
                detail.setItemImage(product.getCoverImage());
                detail.setPrice(product.getPrice());
                detail.setQuantity(entry.getValue());
                detail.setTotalAmount(product.getPrice() * entry.getValue());
                detail.setPointsPrice(product.getPointsPrice());

                details.add(detail);
            }
        } else {
            // 3.2 权益订单(纯积分兑换): 校验与发放复用权益服务公共方法, 下单即兑换唯一入口
            // todo 已知限制: 限兑是查数判断, 并发双击可超限, 后续用Redis setnx锁 exchange:{userId}:{benefitId} 解决
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
        // 4、订单头: 权益订单纯积分即时结算(status=1), 商品订单待支付(status=0)
        LifeOrder order = new LifeOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setOrderType(orderType);
        order.setStatus(orderType == 2 ? 1 : 0);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(orderType == 2 ? 0 : totalAmount);   // TODO 商品积分抵扣上线后 = totalAmount - 抵扣金额
        order.setPointsUsed(pointsUsed);
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
            // 商品订单: 原子扣库存, 0行=被并发抢完, 抛异常触发整体回滚
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
        // 6、组装VO返回
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

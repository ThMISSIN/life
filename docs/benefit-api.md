# 权益模块接口清单

> 版本: v1.0 (2026-09-07)
> 范围: 权益模块 C 端 + 管理端接口设计, 对应表 `life_benefit_category` / `life_benefit` / `life_user_benefit`
> 前置依赖: 积分公共能力 `PointsService.changePoints()`(见文末"公共能力"一节)

---

## 一、公共约定

| 项 | 约定 |
|----|------|
| 响应包装 | `Result<T>` / 分页 `Result<PageResult>`, 与现有 `UserController` 等保持一致 |
| 认证 | C 端接口全部走 `JwtTokenUserInterceptor`, Controller 内用 `BaseContext.getCurrentId()` 取用户 |
| 分页参数 | 继承 `QueryDTO`(pageNum/pageSize/startTime/endTime) |
| 金额单位 | 元 → 分(int), 如 1000 = 10 元; 积分单位为积分原值 |
| 状态码语义 | 业务失败直接 `Result.fail("原因")`, 全局异常走 `GlobalExceptionHandler` |

---

## 二、C 端接口(需登录)

### 2.1 分类

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/benefit/categories` | 分类树(一二级全量, 建议服务端缓存) |

**响应要点**: 树形结构, 仅返回 `status=1` 且未删除的分类, 二级分类内嵌在 `children` 中。

### 2.2 权益浏览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/benefit/page` | 权益分页列表 |
| GET | `/benefit/{id}` | 权益详情 |

**列表查询参数**(`BenefitQueryDTO extends QueryDTO`):

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | Long | 否 | 分类ID(传一级分类时含其下二级的权益) |
| keyword | String | 否 | 名称模糊搜索 |
| sortType | Integer | 否 | 1-综合(默认,按sort) 2-销量 3-积分价格升序 4-积分价格降序 |

**响应要点**: 仅返回 `status=1`(已上架)权益; 列表项含 `id/name/coverImage/pointsPrice/originalPrice/stock/sales`; 详情额外返回 `description/images/benefitType/validType/validDays/fixedEndTime/perUserLimit/minLevel`, 以及**我的已兑次数**(用于前端判断是否达到限兑)。

### 2.3 积分兑换(核心接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/benefit/{id}/exchange` | 用积分兑换权益 |

**请求参数**(`BenefitExchangeDTO`): 无需传积分数量, 单件兑换; `id` 为权益ID。

**服务端校验顺序**(全部通过才进入事务):

1. 权益存在、未删除、`status=1` 已上架
2. `min_level` 满足当前用户会员等级
3. `per_user_limit` 未超限(查 `idx_benefit_user`)
4. `stock != 0` 且余额充足
5. 用户 `points` 余额 >= `points_price`

**事务内动作**(单库本地事务, 顺序即保护顺序):

1. 条件更新扣库存: `UPDATE ... SET stock = stock - 1 WHERE id = ? AND stock >= 1`, 影响行数为 0 → 抛"库存不足"
2. 扣积分: `PointsService.changePoints(-pointsPrice, type=6, orderNo, "积分兑换")`(同时改 `life_user.points` + 写 `life_points_record`)
3. 创建权益订单: `life_order`(order_type=2, pay_amount=0, status=1 已支付)
4. 发放用户权益: 写 `life_user_benefit`(快照名称/封面, 生成全局唯一券码, 按 `valid_type` 计算 `expire_time`, `effective_time` 默认立即生效)
5. `sales + 1`

**响应**(`ExchangeResultVO`): `userBenefitId / orderNo / code / expireTime`。

### 2.4 我的权益

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/benefit/mine/page` | 我的权益分页列表 |
| GET | `/benefit/mine/{id}` | 我的权益详情(含券码) |
| PUT | `/benefit/mine/{id}/use` | 使用(核销) |

**列表查询参数**(`UserBenefitQueryDTO extends QueryDTO`): `status`(Integer, 可选, 0/1/2/3/4)。

**响应要点**: 列表返回快照字段(`benefitName/coverImage/expireTime/status`), **列表不返回券码**, 点进详情才返回; 未到期且未使用的券码展示时可考虑后端返回 + 前端点击显示(一期可直接明文返回)。

**核销规则**: 仅 `status=1`(未使用)且已到 `effective_time` 可核销; 核销 = 状态改 2 + 记录 `verify_time` + `verify_operator` = 本人。乐观校验放在 UPDATE 条件里(`WHERE id=? AND user_id=? AND status=1`)。

---

## 三、管理端接口(一期可后置, 或用若依代码生成器出)

### 3.1 分类管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/benefit/category` | 新增分类 |
| PUT | `/admin/benefit/category` | 更新分类 |
| DELETE | `/admin/benefit/category/{id}` | 删除分类(软删除; 有挂载权益时禁止) |

### 3.2 权益管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/benefit/page` | 权益分页(管理视角, 可见下架数据) |
| POST | `/admin/benefit` | 新增权益 |
| PUT | `/admin/benefit` | 更新权益 |
| PUT | `/admin/benefit/{id}/status` | 上架/下架(`status` 参数, 下架不影响已发放的用户权益) |
| DELETE | `/admin/benefit/{id}` | 删除权益(软删除; 有未核销用户权益时禁止) |

### 3.3 发放记录管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/benefit/records/page` | 发放记录分页(按权益/用户/状态筛选) |
| PUT | `/admin/benefit/records/{id}/invalid` | 作废用户权益 |

**作废规则**: 仅 `status=1` 可作废; 作废 = 状态改 4 + **退积分**(`PointsService.changePoints(+pointsPrice, type=3?, orderNo, "权益作废返还")`) + **回库存**(`stock + 1`) + 写通知。两侧都要写流水, 保证对账可追。

---

## 四、公共能力(非接口, 需在开发权益模块前落地)

### 4.1 积分变动公共方法(地基)

```
PointsService.changePoints(userId, points, type, orderNo, remark)
```

- 同一事务内: 更新 `life_user.points`(余额) + 插入 `life_points_record`(流水)
- 余额不足时抛业务异常
- **现有签到逻辑应切换到该方法**, 保证"改余额必留流水"

### 4.2 过期处理定时任务

- 方案: Spring `@Scheduled`(每小时)扫 `idx_expire_status` 索引, 批量置 `status=3`
- 查询侧惰性兜底: 读到"未使用但已过期"的数据时读时纠正
- 库中已有 qrtz_ 表, 后续任务多了可迁 Quartz, 一期无需

### 4.3 通知(一期最小实现)

- 兑换成功 → 写 `life_user_inbox`(type=4 权益通知); `life_notice_template` 两张表在本阶段一并建好, 模板替换逻辑可后置

---

## 五、前端对接映射(life-frontend)

| 现有 mock 函数(api/mall.js) | 对接接口 | 阶段 |
|------------------------------|----------|------|
| `getProducts()` | `GET /benefit/page` | 权益模块 |
| `redeemProduct(productId)` | `POST /benefit/{id}/exchange` | 权益模块 |
| `getRedemptionHistory()` | `GET /benefit/mine/page` | 权益模块 |
| `getCoupons()` / `claimCoupon()` | 优惠券模块接口 | 优惠券阶段 |
| `getMallBanner()` | `GET /banner/list`(系统模块) | 首页联调阶段 |

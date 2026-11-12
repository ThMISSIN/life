package com.life.constant;

/**
 * Redis Key 常量类
 */
public class RedisConstant {

    /**
     * 用户token黑名单：用于登出后二次校验，防止旧token滥用；value为用户id，key随token剩余有效期自动过期
     */
    public static final String USER_TOKEN_BLACKLIST = "user:token:blacklist:";

    /**
     * 发帖草稿(存PostPublishDTO JSON), 占位符: {userId}
     */
    public static final String POST_DRAFT = "post:draft:uid:{userId}";

    /**
     * 草稿过期天数
     */
    public static final int POST_DRAFT_TTL_DAYS = 30;

    /**
     * 签到bitmap key前缀, 拼接: SIGN_RECORD + userId + yyyy:MM
     */
    public static final String SIGN_RECORD = "sign:uid";

    /**
     * 签到key中的月份格式
     */
    public static final String SIGN_MONTH_FORMAT = "yyyy:MM";

    /**
     * 点赞用户集合(set), 占位符: {bizType}与{bizId}(类型入key, 防多业务id串号与扫描串扰)
     */
    public static final String LIKES_BIZ = "like:set:biz:{bizType}:{bizId}";

    /**
     * 点赞集合key公共前缀, 按类型扫描时拼: LIKES_BIZ_PREFIX + bizType + ":"
     */
    public static final String LIKES_BIZ_PREFIX = "like:set:biz:";

    /**
     * 点赞计数(zset), 占位符: {bizType}
     */
    public static final String LIKES_TIMES = "likes:times:type:{bizType}";

    public static final String CART_ADD_LOCK = "cart:lock:add:{userId}:{itemType}:{itemId}";

    /**
     * 兑换限兑并发锁(2.7): 占位符 {userId}:{benefitId}
     * <p>限兑是"查数判断", 并发双击可超限; 下单前按 用户+权益 维度以 Redisson RLock 请求级互斥:
     * key 字典序逐个获取(全局统一顺序破坏循环等待, 无死锁), 不传 leaseTime 走看门狗自动续期
     * (业务再慢锁也不失效), 任一失败即回滚释放已持有的锁; 锁的获取/释放在事务外层
     * <p>兜底: Redis 锁失效时(如分区后看门狗停摆), 库存/积分/余额为 DB 条件原子扣减(stock>=n),
     * 唯一拦不住的是限兑"查数判断", 依赖对账(biz_no 流水查重)补偿
     */
    public static final String EXCHANGE_LOCK = "exchange:lock:{userId}:{benefitId}";

    /**
     * 商品限购并发锁(2.7): 占位符 {userId}:{productId}, 口径同上
     */
    public static final String PURCHASE_LOCK = "purchase:lock:{userId}:{productId}";

    public static final int ROW_MAX_QUANTITY = 999;

    /**
     * 通知事件 Redis Pub/Sub 频道: life-admin(发布方) -> life-social(订阅方, NoticeEventListener)
     * 一期公告fan-out通道, 二期业务事件通知迁 RabbitMQ 后本通道收敛为仅实时推送触发
     */
    public static final String NOTICE_PUSH_CHANNEL = "life:notice:push";

    /**
     * 下单幂等令牌(状态机 INIT→USED): 占位符 {userId}:{token}, 进入结算/兑换前签发, TTL 内有效
     * <p>提交订单时 Redisson compareAndSet 原子流转 INIT→USED: 并发下仅一个请求能完成流转(防重闸);
     * 下单成功→key 删除(永久失效, 重放一律拒绝); 下单失败→还原 INIT(事务已回滚零副作用, 同令牌可重试);
     * key 含 userId 防跨用户盗用, 同用户多令牌并存互不影响(多标签页安全)
     */
    public static final String ORDER_TOKEN = "order:token:{userId}:{token}";

    /**
     * 令牌初始状态: 签发后/失败还原后的可提交状态
     */
    public static final String ORDER_TOKEN_INIT = "INIT";

    /**
     * 令牌消费中状态: 提交请求持有处理权, 其余并发提交直接判重复
     */
    public static final String ORDER_TOKEN_USED = "USED";

    /**
     * 下单令牌有效期(分钟): 覆盖一次正常结算流程, 又不至于长期堆积
     */
    public static final int ORDER_TOKEN_TTL_MINUTES = 30;

    // ==================== 2.11 热度推荐信息流 ====================

    /**
     * 推荐流第一页 Redis 缓存(L2, 值为实体页 FeedPageSnapshot JSON, 不含登录态字段可全局共享;
     * 页内 liked/favorited 等在缓存之后的组装环节按当前用户实时回填): 占位符 {pageSize}
     */
    public static final String POST_FEED_FIRST_PAGE = "post:feed:hot:p1:s{pageSize}";

    /**
     * 推荐流页缓存清理用扫描前缀(与 POST_FEED_FIRST_PAGE 同步修改)
     */
    public static final String POST_FEED_FIRST_PAGE_PREFIX = "post:feed:hot:p1:s";

    /**
     * 浏览量待回写增量(zset): member=帖子id, score=本轮累计阅读数; 回写任务 ZINCRBY 累计、
     * 排空后 view_count 原子累加(参照点赞 LIKES_TIMES 模式)
     */
    public static final String POST_VIEW_TIMES = "post:view:times";

    /**
     * 阅读去重(防刷): 同一用户对同一帖子每日只计1次; 占位符 {userId}:{postId}:{date}(yyyyMMdd)
     */
    public static final String POST_VIEWED_DEDUP = "post:viewed:uid{userId}:pid{postId}:{date}";

    /**
     * 帖子ID布隆过滤器(Redisson RBloomFilter, 防缓存穿透): 发帖/转发时 add,
     * 单帖读取入口先 mightContain, false 即确定不存在, 不打 DB; 已删id留存在过滤器中只产生可接受误判
     */
    public static final String POST_BLOOM = "post:bloom:ids";

    // ==================== 2.10 风控 ====================

    /**
     * 滑动窗口计数(zset): risk:win:{action}:{userId}, score=时间戳, 窗口外成员即清
     */
    public static final String RISK_WINDOW = "risk:win:{action}:{userId}";

    /**
     * 风险标记(暂停积分发放/点赞等敏感动作): risk:flag:{userId}, TTL 内生效
     */
    public static final String RISK_FLAG = "risk:flag:{userId}";

    // ==================== 2.12 MQ 消费幂等 ====================

    /**
     * 通知消息消费去重: mq:notice:{msgId}, setnx 成功才处理(at-least-once 防重复落库/推送)
     */
    public static final String MQ_NOTICE_DEDUP = "mq:notice:{msgId}";

    // ==================== 2.14 Redis库存 ====================

    /**
     * 商品/权益库存镜像: stock:{itemType}:{itemId} (itemType: product/benefit), 值=DB库存快照
     */
    public static final String STOCK_CACHE = "stock:{itemType}:{itemId}";

    /**
     * 已预热库存的条目注册表(set, member={itemType}:{itemId}), 对账任务遍历比对
     */
    public static final String STOCK_PREHEATED = "stock:preheated";

    // ==================== 4.7 定时任务分布式锁 ====================

    /**
     * 任务锁前缀: task:lock:{taskName}, Redisson tryLock(waitTime=0) 抢不到即跳过本轮(多实例只跑一个)
     */
    public static final String TASK_LOCK = "task:lock:{taskName}";
}

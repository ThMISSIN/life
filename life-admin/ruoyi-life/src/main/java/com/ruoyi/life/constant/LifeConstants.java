package com.ruoyi.life.constant;

/**
 * life业务常量（值与C端 com.life.constant.* 对齐）
 */
public class LifeConstants
{
    /** 通用状态: 启用/正常 */
    public static final int ENABLE = 1;

    /** 通用状态: 禁用 */
    public static final int DISABLE = 0;

    /** 上下架: 上架 */
    public static final int LISTED = 1;

    /** 上下架: 下架 */
    public static final int UNLISTED = 0;

    /** 分类: 顶级父ID */
    public static final long ROOT_PARENT_ID = 0L;

    /** 分类: 一级 */
    public static final int LEVEL_FIRST = 1;

    /** 分类: 二级 */
    public static final int LEVEL_SECOND = 2;

    /** 商品/权益双通道: 无现金价 */
    public static final int NO_CASH_PRICE = 0;

    /** 商品/权益双通道: 无积分价 */
    public static final int NO_POINTS_PRICE = 0;

    /** 库存: 0为不限量口径中的无货值(商品stock=-1表示不限量) */
    public static final int UNLIMITED_STOCK = -1;

    /** 用户权益状态: 待生效 */
    public static final int UB_PENDING = 0;

    /** 用户权益状态: 未使用 */
    public static final int UB_UNUSED = 1;

    /** 用户权益状态: 已使用 */
    public static final int UB_USED = 2;

    /** 用户权益状态: 已过期 */
    public static final int UB_EXPIRED = 3;

    /** 用户权益状态: 已作废 */
    public static final int UB_VOIDED = 4;

    /** 积分流水类型: 兑换作废退回 */
    public static final int POINTS_TYPE_REFUND = 7;

    /** 订单状态: 已支付 */
    public static final int ORDER_PAID = 1;

    /** 订单状态: 已发货 */
    public static final int ORDER_DELIVERED = 2;

    /** 公告状态: 草稿 */
    public static final int ANN_DRAFT = 0;

    /** 公告状态: 已发布 */
    public static final int ANN_PUBLISHED = 1;

    /** 公告状态: 已撤回(下架) */
    public static final int ANN_REVOKED = 2;

    /**
     * 通知事件 Redis Pub/Sub 频道(与C端 life-social NoticeEventListener 订阅口径一致)。
     * 公告在本服务(RuoYi)发布, 用户WebSocket会话在C端服务, 跨进程以事件驱动打通,
     * 二期随路线图 2.12 迁 RabbitMQ。
     */
    public static final String NOTICE_PUSH_CHANNEL = "life:notice:push";

    /** 通知事件类型: 公告发布(触发C端收件箱fan-out) */
    public static final String EVENT_ANNOUNCEMENT_PUBLISHED = "ANNOUNCEMENT_PUBLISHED";
}

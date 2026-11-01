package com.life.service;

import com.life.query.NoticeQueryDTO;
import com.life.result.PageResult;

/**
 * 站内通知服务(1.3): C端收件箱读接口 + 通知写入统一入口
 */
public interface NoticeService {

    /**
     * 通知分页(仅本人收件箱, 支持已读/未读与类型筛选, 时间倒序)
     */
    PageResult queryNoticePage(NoticeQueryDTO query);

    /**
     * 当前用户未读总数
     */
    Long queryUnreadCount();

    /**
     * 单条标记已读: 归属双条件, 非本人/不存在统一报"不存在"防探测; 已读重复调用幂等
     */
    void markRead(Long id);

    /**
     * 全部标记已读
     */
    void markAllRead();

    /**
     * 通知写入统一入口: 落收件箱 + WebSocket 实时推送(离线仅落库, 下次进页面拉取)
     * <p>
     * 一期调用方: 公告发布fan-out(Redis事件驱动)。支付/积分/兑换等业务事件接入见路线图 2.12,
     * 届时按模板 code 渲染 title/content 后调用本方法, 并升级为 MQ 异步。
     *
     * @param userId  接收用户
     * @param type    通知类型, 见 NoticeTypeConstant
     * @param title   已渲染标题
     * @param content 已渲染内容
     * @param bizId   业务ID(可空)
     * @param url     跳转链接(可空)
     */
    void sendNotice(Long userId, Integer type, String title, String content, Long bizId, String url);

    /**
     * 系统公告发布fan-out: 全量正常用户收件箱插入 + 在线用户实时推送
     * <p>
     * 由管理端(life-admin/RuoYi)发布公告后经 Redis Pub/Sub(life:notice:push) 触发;
     * 按 bizId+type 幂等, 重复事件不会二次fan-out。一期全量fan-out适用于当前用户体量,
     * 用户量上来后迁移 MQ 分批投递(路线图 2.12)。
     *
     * @param announcementId 公告ID
     * @param title          公告标题
     * @param content        公告内容
     * @return fan-out用户数(幂等跳过时为0)
     */
    int fanOutAnnouncement(Long announcementId, String title, String content);
}

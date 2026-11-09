package com.life.service;

import java.util.Collection;

/**
 * 2.11 热度分服务: 公式重算(单帖/批量/衰减扫描) + 阅读上报与回写
 */
public interface PostHotService {

    /**
     * 重算单帖热度(发帖初始化/评论增删/转发后调用; 单语句原子, 计数取行内现值)
     */
    void recalc(Long postId);

    /**
     * 批量重算热度(点赞回写任务搭车)
     */
    void recalcBatch(Collection<Long> postIds);

    /**
     * 衰减扫描: 重算发布在窗口内的已发布帖, 让时间衰减持续生效、新帖24h扶持到期摘除
     *
     * @return 本轮重算行数
     */
    int sweepRecentPosts();

    /**
     * 阅读上报(2.11 "阅"权重数据源): 布隆挡不存在id → 按 用户+帖+日 去重防刷 →
     * Redis ZSet 累计增量, 由回写任务落 view_count; 全程异步口径, 上报失败不影响前端
     *
     * @return 本次是否计入(去重后)
     */
    boolean recordView(Long postId, Long userId);

    /**
     * 排空阅读增量ZSet回写 view_count 并批量刷新热度(定时任务调用)
     *
     * @return 本轮回写帖子数
     */
    int drainViewTimes();
}

package com.life.service;

import com.life.entity.LifePost;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 2.11 ES 全文检索(替换 SQL LIKE '%kw%'): 低层 REST 直连(不绑定客户端版本),
 * 应用双写同步(发帖/删帖/转发/审核过审即写索引, 决策: 不上 Canal——免动 MySQL binlog 配置与新增容器);
 * ES 不可用时全链路自动降级回 SQL LIKE, 搜索功能不中断
 */
public interface PostSearchService {

    /**
     * ES 可用性(30s 缓存探测): false 时调用方走 SQL LIKE 降级
     */
    boolean available();

    /**
     * 幂等建索引(启动异步执行): 优先 ik_max_word 分词, 插件缺失自动回退 standard
     */
    void ensureIndex();

    /**
     * 关键词搜索: 返回按相关度排序的帖子id页; ES 不可用返回 null(调用方降级)
     */
    SearchResult search(String keyword, int pageNum, int pageSize);

    /**
     * 写入/更新索引文档(发帖/转发/审核通过/计数刷新后调用, 尽力而为不抛异常)
     */
    void indexPost(LifePost post);

    /**
     * 移除索引文档(删帖/审核驳回)
     */
    void removePost(Long postId);

    /**
     * 全量重建索引(管理端触发): 分页扫描已发布帖 _bulk 灌入
     *
     * @return 索引文档数
     */
    int reindexAll();

    /**
     * 搜索结果页
     */
    @Data
    @AllArgsConstructor
    class SearchResult {
        private long total;
        private List<Long> ids;
    }
}

package com.ruoyi.life.mapper;

import com.ruoyi.life.domain.LifeAnnouncement;
import com.ruoyi.life.domain.query.LifeAnnouncementQuery;

import java.util.List;

/**
 * 系统公告 数据层
 */
public interface LifeAnnouncementMapper
{
    /** 分页查询公告列表(不分页由Controller startPage接管) */
    public List<LifeAnnouncement> selectLifeAnnouncementList(LifeAnnouncementQuery query);

    /** 按ID查询 */
    public LifeAnnouncement selectLifeAnnouncementById(Long id);

    /** 新增 */
    public int insertLifeAnnouncement(LifeAnnouncement lifeAnnouncement);

    /** 修改 */
    public int updateLifeAnnouncement(LifeAnnouncement lifeAnnouncement);

    /** 发布: 状态置1并写发布时间(仅草稿/已撤回可发, 条件更新防并发双发布) */
    public int publishById(Long id, java.util.Date publishTime, Long updater);

    /** 下架(撤回): 状态1置2(条件更新防并发) */
    public int revokeById(Long id, Long updater);

    /** 逻辑删除 */
    public int deleteLifeAnnouncementById(Long id);
}

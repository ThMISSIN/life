package com.ruoyi.life.service;

import com.ruoyi.life.domain.LifeAnnouncement;
import com.ruoyi.life.domain.query.LifeAnnouncementQuery;

import java.util.List;

/**
 * 系统公告管理 服务接口
 */
public interface ILifeAnnouncementService
{
    /** 分页查询(status/type不传查全部); 分页由Controller startPage()接管 */
    public List<LifeAnnouncement> selectLifeAnnouncementList(LifeAnnouncementQuery query);

    /** 按ID查询 */
    public LifeAnnouncement selectLifeAnnouncementById(Long id);

    /** 新增公告(默认草稿) */
    public int insertLifeAnnouncement(LifeAnnouncement lifeAnnouncement);

    /** 修改公告: 部分更新, 已发布公告需先撤回再编辑 */
    public int updateLifeAnnouncement(LifeAnnouncement lifeAnnouncement);

    /**
     * 发布公告: 草稿/已撤回 -> 已发布(写发布时间), 并向C端推送收件箱fan-out事件
     */
    public int publish(Long id);

    /**
     * 下架(撤回): 已发布 -> 已撤回
     */
    public int revoke(Long id);

    /** 删除公告(软删除, 已发布禁止) */
    public int deleteLifeAnnouncementById(Long id);
}

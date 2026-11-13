package com.ruoyi.life.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.life.constant.LifeConstants;
import com.ruoyi.life.domain.LifeAnnouncement;
import com.ruoyi.life.domain.query.LifeAnnouncementQuery;
import com.ruoyi.life.mapper.LifeAnnouncementMapper;
import com.ruoyi.life.service.ILifeAnnouncementService;
import com.ruoyi.life.util.LifeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统公告管理 服务实现
 * <p>
 * 架构决策(1.3): 公告发布动作只在本服务(RuoYi)落库, C端用户的实时推送
 * 通过 Redis Pub/Sub 事件交给 life-social 处理(其持有用户WebSocket会话与收件箱),
 * 不把WebSocket代码搬进管理端。二期随路线图 2.12 迁 RabbitMQ。
 *
 * @author life
 */
@Service
public class LifeAnnouncementServiceImpl implements ILifeAnnouncementService
{
    private static final Logger log = LoggerFactory.getLogger(LifeAnnouncementServiceImpl.class);

    @Autowired
    private LifeAnnouncementMapper lifeAnnouncementMapper;

    /**
     * String序列化通道(Boot自动配置), 保证报文体为裸JSON串, 与C端监听器解析口径一致
     */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<LifeAnnouncement> selectLifeAnnouncementList(LifeAnnouncementQuery query)
    {
        return lifeAnnouncementMapper.selectLifeAnnouncementList(query);
    }

    @Override
    public LifeAnnouncement selectLifeAnnouncementById(Long id)
    {
        return lifeAnnouncementMapper.selectLifeAnnouncementById(id);
    }

    /**
     * 新增公告: 标题/内容必填, 默认草稿; 允许表单直接选择发布(与C端权益"存为草稿/直接发布"同交互)
     */
    @Override
    public int insertLifeAnnouncement(LifeAnnouncement lifeAnnouncement)
    {
        if (lifeAnnouncement.getTitle() == null || lifeAnnouncement.getTitle().isBlank())
        {
            throw new ServiceException("公告标题不能为空");
        }
        if (lifeAnnouncement.getContent() == null || lifeAnnouncement.getContent().isBlank())
        {
            throw new ServiceException("公告内容不能为空");
        }
        if (lifeAnnouncement.getType() == null || lifeAnnouncement.getType() < 1 || lifeAnnouncement.getType() > 3)
        {
            throw new ServiceException("公告类型不合法(1-系统 2-活动 3-权益)");
        }
        if (lifeAnnouncement.getStatus() == null)
        {
            lifeAnnouncement.setStatus(LifeConstants.ANN_DRAFT);
        }
        if (lifeAnnouncement.getTop() == null)
        {
            lifeAnnouncement.setTop(0);
        }
        boolean publishNow = LifeConstants.ANN_PUBLISHED == lifeAnnouncement.getStatus();
        if (publishNow)
        {
            lifeAnnouncement.setPublishTime(LifeUtils.now());
        }
        lifeAnnouncement.setCreateTime(LifeUtils.now());
        lifeAnnouncement.setUpdateTime(LifeUtils.now());
        lifeAnnouncement.setCreater(LifeUtils.currentOperator());
        lifeAnnouncement.setUpdater(LifeUtils.currentOperator());
        lifeAnnouncement.setDeleted(0);
        int rows = lifeAnnouncementMapper.insertLifeAnnouncement(lifeAnnouncement);
        // 新增即发布: 新记录无并发双发风险, 落库成功后直接推fan-out事件
        if (rows > 0 && publishNow)
        {
            pushAnnouncementPublishedEvent(lifeAnnouncement);
        }
        return rows;
    }

    /**
     * 修改公告: 已发布公告禁止直接编辑(收件箱已fan-out旧文案), 需先撤回
     */
    @Override
    public int updateLifeAnnouncement(LifeAnnouncement lifeAnnouncement)
    {
        if (lifeAnnouncement.getId() == null)
        {
            throw new ServiceException("公告ID不能为空");
        }
        LifeAnnouncement origin = lifeAnnouncementMapper.selectLifeAnnouncementById(lifeAnnouncement.getId());
        if (origin == null)
        {
            throw new ServiceException("公告不存在");
        }
        if (origin.getStatus() != null && origin.getStatus() == LifeConstants.ANN_PUBLISHED)
        {
            throw new ServiceException("公告已发布, 请先撤回再修改");
        }
        lifeAnnouncement.setUpdateTime(LifeUtils.now());
        lifeAnnouncement.setUpdater(LifeUtils.currentOperator());
        return lifeAnnouncementMapper.updateLifeAnnouncement(lifeAnnouncement);
    }

    /**
     * 发布: 条件更新仅草稿/已撤回可发, 成功后推 Redis 事件触发C端收件箱fan-out;
     * 事件发送失败仅告警不回滚(公告本体已生效, C端列表仍可通过已发布状态查询)
     */
    @Override
    public int publish(Long id)
    {
        LifeAnnouncement announcement = lifeAnnouncementMapper.selectLifeAnnouncementById(id);
        if (announcement == null)
        {
            throw new ServiceException("公告不存在");
        }
        if (announcement.getStatus() != null && announcement.getStatus() == LifeConstants.ANN_PUBLISHED)
        {
            throw new ServiceException("公告已是发布状态");
        }
        int rows = lifeAnnouncementMapper.publishById(id, LifeUtils.now(), LifeUtils.currentOperator());
        if (rows == 0)
        {
            throw new ServiceException("公告状态已变更, 请刷新后重试");
        }
        announcement.setStatus(LifeConstants.ANN_PUBLISHED);
        pushAnnouncementPublishedEvent(announcement);
        return rows;
    }

    /**
     * 下架(撤回): 条件更新仅已发布可撤
     */
    @Override
    public int revoke(Long id)
    {
        int rows = lifeAnnouncementMapper.revokeById(id, LifeUtils.currentOperator());
        if (rows == 0)
        {
            throw new ServiceException("公告不存在或不是已发布状态");
        }
        return rows;
    }

    @Override
    public int deleteLifeAnnouncementById(Long id)
    {
        LifeAnnouncement announcement = lifeAnnouncementMapper.selectLifeAnnouncementById(id);
        if (announcement == null)
        {
            throw new ServiceException("公告不存在");
        }
        if (announcement.getStatus() != null && announcement.getStatus() == LifeConstants.ANN_PUBLISHED)
        {
            throw new ServiceException("已发布公告不能删除, 请先下架(撤回)");
        }
        return lifeAnnouncementMapper.deleteLifeAnnouncementById(id);
    }

    /**
     * 发布事件报文: {"event":"ANNOUNCEMENT_PUBLISHED","announcementId":1,"title":"...","content":"..."}
     */
    private void pushAnnouncementPublishedEvent(LifeAnnouncement announcement)
    {
        try
        {
            JSONObject event = new JSONObject();
            event.put("event", LifeConstants.EVENT_ANNOUNCEMENT_PUBLISHED);
            event.put("announcementId", announcement.getId());
            event.put("title", announcement.getTitle());
            event.put("content", announcement.getContent());
            stringRedisTemplate.convertAndSend(LifeConstants.NOTICE_PUSH_CHANNEL, event.toJSONString());
        }
        catch (Exception e)
        {
            log.error("公告fan-out事件推送失败(Redis不可用?), announcementId={}", announcement.getId(), e);
        }
    }
}

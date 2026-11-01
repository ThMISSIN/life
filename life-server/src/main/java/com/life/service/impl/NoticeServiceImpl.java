package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.NoticeTypeConstant;
import com.life.constant.StatusConstant;
import com.life.context.BaseContext;
import com.life.entity.LifeUser;
import com.life.entity.LifeUserInbox;
import com.life.exception.ValidationException;
import com.life.mapper.LifeUserInboxMapper;
import com.life.mapper.UserMapper;
import com.life.query.NoticeQueryDTO;
import com.life.result.PageResult;
import com.life.service.NoticeService;
import com.life.utils.BeanUtils;
import com.life.vo.NoticePushVO;
import com.life.vo.NoticeVO;
import com.life.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 站内通知服务实现
 */
@Service
@Slf4j
public class NoticeServiceImpl extends ServiceImpl<LifeUserInboxMapper, LifeUserInbox> implements NoticeService {

    /**
     * 公告在收件箱中的跳转路径(二期公告详情页挂此路由)
     */
    private static final String ANNOUNCEMENT_URL = "/notice/announcement?id=";

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResult queryNoticePage(NoticeQueryDTO query) {
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<LifeUserInbox> wrapper = new LambdaQueryWrapper<LifeUserInbox>()
                .eq(LifeUserInbox::getUserId, userId)
                .eq(query.getIsRead() != null, LifeUserInbox::getIsRead, query.getIsRead())
                .eq(query.getType() != null, LifeUserInbox::getType, query.getType())
                .orderByDesc(LifeUserInbox::getCreateTime)
                .orderByDesc(LifeUserInbox::getId);
        Page<LifeUserInbox> page = this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<NoticeVO> records = BeanUtils.copyList(page.getRecords(), NoticeVO.class);
        return new PageResult(page.getTotal(), records);
    }

    @Override
    public Long queryUnreadCount() {
        return countUnread(BaseContext.getCurrentId());
    }

    @Override
    public void markRead(Long id) {
        Long userId = BaseContext.getCurrentId();
        // 归属双条件(与购物车口径一致): 非本人一律报"不存在", 防越权探测
        long owned = this.count(new LambdaQueryWrapper<LifeUserInbox>()
                .eq(LifeUserInbox::getId, id)
                .eq(LifeUserInbox::getUserId, userId));
        if (owned == 0) {
            throw new ValidationException("通知不存在");
        }
        this.update(new LambdaUpdateWrapper<LifeUserInbox>()
                .eq(LifeUserInbox::getId, id)
                .eq(LifeUserInbox::getUserId, userId)
                .eq(LifeUserInbox::getIsRead, 0)
                .set(LifeUserInbox::getIsRead, 1));
        // 多端未读角标同步
        pushUnread(userId);
    }

    @Override
    public void markAllRead() {
        Long userId = BaseContext.getCurrentId();
        this.update(new LambdaUpdateWrapper<LifeUserInbox>()
                .eq(LifeUserInbox::getUserId, userId)
                .eq(LifeUserInbox::getIsRead, 0)
                .set(LifeUserInbox::getIsRead, 1));
        pushUnread(userId);
    }

    @Override
    public void sendNotice(Long userId, Integer type, String title, String content, Long bizId, String url) {
        LifeUserInbox inbox = LifeUserInbox.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .bizId(bizId)
                .url(url)
                .isRead(0)
                .build();
        save(inbox);
        // 在线实时推送; 失败不影响落库
        NoticeVO notice = BeanUtils.copyBean(inbox, NoticeVO.class);
        webSocketServer.sendToUser(userId, NoticePushVO.builder()
                .event("notice")
                .notice(notice)
                .unreadCount(countUnread(userId))
                .build());
    }

    @Override
    public int fanOutAnnouncement(Long announcementId, String title, String content) {
        // 幂等: 同公告已fan-out过(如管理端重复发布)直接跳过
        long exists = this.count(new LambdaQueryWrapper<LifeUserInbox>()
                .eq(LifeUserInbox::getType, NoticeTypeConstant.SYSTEM)
                .eq(LifeUserInbox::getBizId, announcementId));
        if (exists > 0) {
            log.info("公告已fan-out过, 跳过: announcementId={}", announcementId);
            return 0;
        }
        List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<LifeUser>()
                        .select(LifeUser::getId)
                        .eq(LifeUser::getStatus, StatusConstant.ENABLE))
                .stream().map(LifeUser::getId).collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return 0;
        }
        List<LifeUserInbox> rows = userIds.stream()
                .map(userId -> LifeUserInbox.builder()
                        .userId(userId)
                        .type(NoticeTypeConstant.SYSTEM)
                        .title(title)
                        .content(content)
                        .bizId(announcementId)
                        .url(ANNOUNCEMENT_URL + announcementId)
                        .isRead(0)
                        .build())
                .collect(Collectors.toList());
        saveBatch(rows);

        // 仅向在线用户实时推送, 离线用户下次进入通知列表可见
        Set<Long> fanOutUserIds = new HashSet<>(userIds);
        for (Long userId : webSocketServer.onlineUserIds()) {
            if (!fanOutUserIds.contains(userId)) {
                continue;
            }
            webSocketServer.sendToUser(userId, NoticePushVO.builder()
                    .event("notice")
                    .notice(findNoticeRow(userId, announcementId))
                    .unreadCount(countUnread(userId))
                    .build());
        }
        log.info("公告fan-out完成: announcementId={}, 覆盖用户数={}", announcementId, rows.size());
        return rows.size();
    }

    /**
     * 取该用户的公告收件箱行(推送报文需带各自行id, 供前端单条已读)
     */
    private NoticeVO findNoticeRow(Long userId, Long announcementId) {
        LifeUserInbox own = this.getOne(new LambdaQueryWrapper<LifeUserInbox>()
                .eq(LifeUserInbox::getUserId, userId)
                .eq(LifeUserInbox::getType, NoticeTypeConstant.SYSTEM)
                .eq(LifeUserInbox::getBizId, announcementId)
                .last("limit 1"));
        return own == null ? null : BeanUtils.copyBean(own, NoticeVO.class);
    }

    /**
     * 未读数: idx_user_id + idx_is_read 组合过滤, 当前体量直查DB, 后续量大再上Redis计数
     */
    private Long countUnread(Long userId) {
        return this.count(new LambdaQueryWrapper<LifeUserInbox>()
                .eq(LifeUserInbox::getUserId, userId)
                .eq(LifeUserInbox::getIsRead, 0));
    }

    private void pushUnread(Long userId) {
        webSocketServer.sendToUser(userId, NoticePushVO.builder()
                .event("unread")
                .unreadCount(countUnread(userId))
                .build());
    }
}

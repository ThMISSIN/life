package com.life.controller;

import com.life.query.NoticeQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * C端站内通知接口(均需登录, /notice/** 已注册拦截器)
 */
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 通知分页(已读/未读 isRead 与类型 type 可选筛选)
     */
    @GetMapping("/page")
    public Result<PageResult> page(NoticeQueryDTO query) {
        return Result.success(noticeService.queryNoticePage(query));
    }

    /**
     * 未读总数(角标)
     */
    @GetMapping("/unread/count")
    public Result<Long> unreadCount() {
        return Result.success(noticeService.queryUnreadCount());
    }

    /**
     * 单条标记已读
     */
    @PutMapping("/{id}/read")
    public Result markRead(@PathVariable Long id) {
        noticeService.markRead(id);
        return Result.success();
    }

    /**
     * 全部标记已读
     */
    @PutMapping("/read/all")
    public Result markAllRead() {
        noticeService.markAllRead();
        return Result.success();
    }
}

package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.dto.PostPublishDTO;
import com.life.entity.LifePost;
import com.life.query.PostQueryDTO;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.vo.PostVO;

public interface PostService extends IService<LifePost> {
    // 发布帖子, 返回完整PostVO供前端插入信息流; 成功后自动清理当前用户草稿
    PostVO publishPost(PostPublishDTO postDTO);

    // 帖子分页列表: 信息流(type 1推荐/2关注)或指定用户动态(userId), 组装作者/点赞/关注状态
    PageResult pageQuery(PostQueryDTO query);

    // 删除帖子(校验归属, 逻辑删除)
    void deletePostById(Long postId);

    // 保存/更新草稿(一人一份: 无草稿则新建, 有则覆盖), 返回草稿VO含id; 空内容视为放弃草稿
    PostVO saveDraft(PostPublishDTO postDTO);

    // 获取当前用户最新草稿, 无草稿返回null(正常态, 不抛异常)
    PostVO getDraft();

    // 清理当前用户全部草稿(幂等, 无草稿不报错)
    void clearDraft();

}

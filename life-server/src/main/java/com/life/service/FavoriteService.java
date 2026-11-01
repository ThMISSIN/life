package com.life.service;

import com.life.dto.FavoriteToggleDTO;
import com.life.query.FavoriteQueryDTO;
import com.life.result.PageResult;

import java.util.List;

/**
 * 收藏服务(1.4): 帖子/权益/商品跨业务收藏
 */
public interface FavoriteService {

    /**
     * 收藏/取消 toggle: 未收藏则新增, 已收藏则取消(逻辑删行复活而非重插, 规避 uk_user_biz)
     *
     * @return true=收藏成功, false=已取消
     */
    Boolean toggle(FavoriteToggleDTO toggleDTO);

    /**
     * 收藏分页(仅本人, 可按bizType筛选): 目标名称/图片实时回填, 失效目标 valid=false
     */
    PageResult queryFavoritePage(FavoriteQueryDTO query);

    /**
     * 批量收藏状态(列表页角标): 返回入参ids中已被当前用户收藏的子集
     * (参照 LikeService.isBizLiked 的批量口径, 收藏无热存需求直查DB唯一键索引)
     */
    List<Long> listFavoritedBizIds(Integer bizType, List<Long> bizIds);
}

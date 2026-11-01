package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.life.entity.LifeFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LifeFavoriteMapper extends BaseMapper<LifeFavorite> {

    /**
     * 查任意状态收藏行(含逻辑删), 供 toggle 判断"新增/复活/取消"
     * <p>绕过 MP 的 deleted=0 过滤, 直查物理行
     */
    @Select("select id, user_id, biz_id, biz_type, deleted from life_favorite " +
            "where user_id = #{userId} and biz_id = #{bizId} and biz_type = #{bizType} limit 1")
    LifeFavorite selectAnyByUserBiz(@Param("userId") Long userId,
                                    @Param("bizId") Long bizId,
                                    @Param("bizType") Integer bizType);

    /**
     * 复活已取消(deleted=1)的收藏行, 规避唯一键冲突
     */
    @Update("update life_favorite set deleted = 0, update_time = now() where id = #{id} and deleted = 1")
    int revive(@Param("id") Long id);

    /**
     * 逻辑删除收藏(取消收藏)
     */
    @Update("update life_favorite set deleted = 1, update_time = now() " +
            "where user_id = #{userId} and biz_id = #{bizId} and biz_type = #{bizType} and deleted = 0")
    int logicDeleteByUserBiz(@Param("userId") Long userId,
                             @Param("bizId") Long bizId,
                             @Param("bizType") Integer bizType);
}

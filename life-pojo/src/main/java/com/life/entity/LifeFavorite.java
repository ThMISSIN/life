package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收藏表(跨业务: 帖子/权益/商品; uk_user_biz(user_id,biz_id,biz_type) 唯一键防重)
 * <p>
 * 注意: 唯一键不含 deleted —— 取消收藏(逻辑删)后重新收藏必须"复活原行"(UPDATE deleted=0),
 * 不能再 INSERT, 否则撞唯一键; toggle 语义在 FavoriteServiceImpl 处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_favorite")
public class LifeFavorite extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 业务ID(帖子ID/权益ID/商品ID)
     */
    private Long bizId;

    /**
     * 业务类型: 1-帖子, 2-权益, 3-商品(见 FavoriteTypeConstant)
     */
    private Integer bizType;
}

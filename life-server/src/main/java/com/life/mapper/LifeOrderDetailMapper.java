package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.life.entity.LifeOrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LifeOrderDetailMapper extends BaseMapper<LifeOrderDetail> {

    /**
     * 统计用户对某商品的已购数量(排除已取消/已退款订单), 用于商品限购校验
     */
    @Select("select ifnull(sum(d.quantity), 0) from life_social.life_order_detail d " +
            "join life_social.life_order o on o.id = d.order_id and o.deleted = 0 " +
            "where d.item_type = 1 and d.item_id = #{productId} and o.user_id = #{userId} " +
            "and o.status not in (4, 5) and d.deleted = 0")
    long countPurchased(Long userId, Long productId);
}

package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.life.entity.LifeBalanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LifeBalanceRecordMapper extends BaseMapper<LifeBalanceRecord> {

    /**
     * 资金对账(4.9): 某用户余额流水净额(应等于 life_user.money)
     */
    @Select("SELECT IFNULL(SUM(amount), 0) FROM life_balance_record WHERE user_id = #{userId} AND deleted = 0")
    Long sumAmount(@Param("userId") Long userId);
}

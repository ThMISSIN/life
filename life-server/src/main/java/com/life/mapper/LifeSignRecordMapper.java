package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.life.entity.LifeSignRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface LifeSignRecordMapper extends BaseMapper<LifeSignRecord> {

    /**
     * 签到落库(3.5): INSERT IGNORE + uk_user_date 唯一键天然幂等,
     * 重复签到/MQ重投/Redis丢数据补写都只留一行
     */
    @Insert("INSERT IGNORE INTO life_sign_record (user_id, sign_date, continue_days, reward_points, creater) " +
            "VALUES (#{userId}, #{signDate}, #{continueDays}, #{rewardPoints}, #{userId})")
    int insertIgnore(@Param("userId") Long userId, @Param("signDate") LocalDate signDate,
                     @Param("continueDays") int continueDays, @Param("rewardPoints") int rewardPoints);
}

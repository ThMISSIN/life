package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifePointsRecord;
import com.life.query.QueryDTO;
import com.life.vo.PointsRankVO;
import com.life.vo.PointsSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LifePointsRecordMapper extends BaseMapper<LifePointsRecord> {

    Page<LifePointsRecord> queryList(Page<LifePointsRecord> page,
                                     @Param("userId") Long userId,
                                     @Param("queryDTO") QueryDTO queryDTO);

    /**
     * 聚合某用户自 monthStart 起的积分获得/消耗(单次扫描):
     * 正数流水之和 = earned, 负数流水之和的绝对值 = spent; 无流水时 ifnull 兜底为 0。
     * 只回填 monthEarned/monthSpent 两字段, total(余额)由服务层以 life_user.points 账户字段为准。
     * 注: 注解 SQL 不走 MP 逻辑删自动过滤, deleted = 0 需手写;
     *     无 GROUP BY 的聚合查询恒返回一行, 返回值不会为 null
     */
    @Select("select ifnull(sum(case when points > 0 then points else 0 end), 0) as monthEarned, " +
            "ifnull(sum(case when points < 0 then -points else 0 end), 0) as monthSpent " +
            "from life_points_record " +
            "where user_id = #{userId} and deleted = 0 and create_time >= #{monthStart}")
    PointsSummaryVO sumPointsSince(@Param("userId") Long userId, @Param("monthStart") LocalDateTime monthStart);

    /**
     * 积分榜单聚合(半闭区间 [start, end)): 区间内正数流水按用户求和倒序取前 limit 名,
     * 榜单分页与月底结算 top3 复用同一口径; join life_user 补 nickname/username/avatar,
     * nickname 为空时以 username 兜底; rank 由服务层按 offset+行序回填, SQL 不必计算名次
     * (SQL 实现注解 @Select 或 XML 均可, 口径参考 sumPointsSince: deleted = 0 需手写;
     *  聚合建议依赖 life_points_record 的 create_time 索引)
     */
    List<PointsRankVO> selectMonthRankBoard(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("offset") Integer offset,
                                            @Param("limit") Integer limit);

    /**
     * 我的名次: 同期正数流水求和后 sum 比我大的人数 + 1;
     * 无流水/未上榜时返回 0, 由服务层决定展示口径
     */
    Integer selectMyMonthRank(@Param("userId") Long userId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);
}

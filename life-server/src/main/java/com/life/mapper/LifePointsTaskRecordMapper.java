package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.life.entity.LifePointsTaskRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface LifePointsTaskRecordMapper extends BaseMapper<LifePointsTaskRecord> {

    /**
     * 当日某任务已完成次数(撞 uk_user_task_date_seq 判定限次, 重复事件静默失败)
     */
    @Select("SELECT COUNT(*) FROM life_points_task_record " +
            "WHERE user_id = #{userId} AND task_id = #{taskId} AND task_date = #{date} AND deleted = 0")
    int countToday(@Param("userId") Long userId, @Param("taskId") Long taskId, @Param("date") LocalDate date);

    /**
     * 领取(2.4): 条件更新 status 0→1, 并发双击只成功一次; 返回0=已被领取或不存在
     */
    @Update("UPDATE life_points_task_record SET status = 1, update_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = 0 AND deleted = 0")
    int markClaimed(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 用户最早一条待领取记录(claim 按 taskId 领取, 一次性任务的历史完成也可领)
     */
    @Select("SELECT * FROM life_points_task_record " +
            "WHERE user_id = #{userId} AND task_id = #{taskId} AND status = 0 AND deleted = 0 " +
            "ORDER BY task_date ASC, seq ASC LIMIT 1")
    LifePointsTaskRecord selectFirstClaimable(@Param("userId") Long userId, @Param("taskId") Long taskId);

    /**
     * 任务列表进度回填: 一批任务的当日完成数与待领取数(两条聚合SQL, 服务层组装)
     */
    @Select("<script>SELECT task_id AS taskId, COUNT(*) AS cnt FROM life_points_task_record " +
            "WHERE user_id = #{userId} AND task_date = #{date} AND deleted = 0 " +
            "AND task_id IN <foreach collection='taskIds' item='tid' open='(' separator=',' close=')'>#{tid}</foreach> " +
            "GROUP BY task_id</script>")
    List<java.util.Map<String, Object>> countTodayByTasks(@Param("userId") Long userId,
                                                          @Param("date") LocalDate date,
                                                          @Param("taskIds") List<Long> taskIds);

    @Select("<script>SELECT task_id AS taskId, COUNT(*) AS cnt FROM life_points_task_record " +
            "WHERE user_id = #{userId} AND status = 0 AND deleted = 0 " +
            "AND task_id IN <foreach collection='taskIds' item='tid' open='(' separator=',' close=')'>#{tid}</foreach> " +
            "GROUP BY task_id</script>")
    List<java.util.Map<String, Object>> countClaimableByTasks(@Param("userId") Long userId,
                                                              @Param("taskIds") List<Long> taskIds);
}

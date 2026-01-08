package com.unlimited.sports.globox.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.coach.entity.CoachCustomSchedule;
import com.unlimited.sports.globox.model.coach.entity.CoachSlotBatchOperationLog;
import com.unlimited.sports.globox.model.coach.entity.CoachSlotRecord;
import com.unlimited.sports.globox.model.coach.entity.CoachSlotTemplate;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * @since 2026/1/3 14:32
 *
 */
@Mapper
public interface CoachSlotTemplateMapper extends BaseMapper<CoachSlotTemplate> {

    /**
     * 统计时段冲突数量
     */
    @Select("SELECT COUNT(*) FROM coach_slot_template " +
            "WHERE coach_user_id = #{coachUserId} " +
            "AND is_deleted = 0 " +
            "AND coach_slot_template_id != #{excludeId} " +
            "AND ((start_time < #{endTime} AND end_time > #{startTime}))")
    int countConflicts(
            @Param("coachUserId") Long coachUserId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    /**
     * 根据教练ID查询所有未删除的模板
     */
    @Select("SELECT * FROM coach_slot_template " +
            "WHERE coach_user_id = #{coachUserId} " +
            "AND is_deleted = 0 " +
            "ORDER BY start_time")
    List<CoachSlotTemplate> selectByCoachUserId(@Param("coachUserId") Long coachUserId);
}
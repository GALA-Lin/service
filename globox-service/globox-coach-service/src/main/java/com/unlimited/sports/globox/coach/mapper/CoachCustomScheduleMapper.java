package com.unlimited.sports.globox.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.coach.entity.CoachCustomSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * @since 2026/1/3 14:37
 *
 */
@Mapper
public interface CoachCustomScheduleMapper extends BaseMapper<CoachCustomSchedule> {

    /**
     * 查询指定日期范围内的自定义日程
     */
    @Select("SELECT * FROM coach_custom_schedule " +
            "WHERE coach_user_id = #{coachUserId} " +
            "AND schedule_date BETWEEN #{startDate} AND #{endDate} " +
            "AND status = 1 " +
            "ORDER BY schedule_date, start_time")
    List<CoachCustomSchedule> selectByDateRange(
            @Param("coachUserId") Long coachUserId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 检查自定义日程冲突
     */
    @Select("SELECT COUNT(*) FROM coach_custom_schedule " +
            "WHERE coach_user_id = #{coachUserId} " +
            "AND schedule_date = #{date} " +
            "AND status = 1 " +
            "AND coach_custom_schedule_id != #{excludeId} " +
            "AND (" +
            "  (start_time < #{endTime} AND end_time > #{startTime})" +
            ")")
    int countScheduleConflicts(
            @Param("coachUserId") Long coachUserId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );
}

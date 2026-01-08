package com.unlimited.sports.globox.coach.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    /**
     * 根据日期范围查询自定义日程
     */
    default List<CoachCustomSchedule> selectByDateRange(
            Long coachUserId,
            LocalDate startDate,
            LocalDate endDate) {
        return selectList(new LambdaQueryWrapper<CoachCustomSchedule>()
                .eq(CoachCustomSchedule::getCoachUserId, coachUserId)
                .between(CoachCustomSchedule::getScheduleDate, startDate, endDate)
                .eq(CoachCustomSchedule::getStatus, 1)
                .orderByAsc(CoachCustomSchedule::getScheduleDate)
                .orderByAsc(CoachCustomSchedule::getStartTime));
    }

}

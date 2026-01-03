package com.unlimited.sports.globox.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.coach.entity.CoachSlotRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @since 2026/1/3 14:34
 *
 */
@Mapper
public interface CoachSlotRecordMapper extends BaseMapper<CoachSlotRecord> {

    /**
     * 查询指定日期范围内的时段记录
     */
    @Select("SELECT * FROM coach_slot_record " +
            "WHERE coach_user_id = #{coachUserId} " +
            "AND booking_date BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY booking_date, start_time")
    List<CoachSlotRecord> selectByDateRange(
            @Param("coachUserId") Long coachUserId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 查询指定模板在指定日期的记录
     */
    @Select("SELECT * FROM coach_slot_record " +
            "WHERE coach_slot_template_id = #{templateId} " +
            "AND booking_date = #{bookingDate}")
    CoachSlotRecord selectByTemplateIdAndDate(
            @Param("templateId") Long templateId,
            @Param("bookingDate") LocalDate bookingDate
    );

    /**
     * 批量查询指定日期范围内的记录
     */
    @Select("<script>" +
            "SELECT * FROM coach_slot_record " +
            "WHERE coach_slot_template_id IN " +
            "<foreach collection='templateIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "AND booking_date BETWEEN #{startDate} AND #{endDate}" +
            "</script>")
    List<CoachSlotRecord> selectByTemplateIdsAndDateRange(
            @Param("templateIds") List<Long> templateIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 原子性锁定时段
     */
    @Select("UPDATE coach_slot_record SET " +
            "status = #{newStatus}, " +
            "locked_by_user_id = #{userId}, " +
            "locked_until = #{lockedUntil}, " +
            "locked_type = 1, " +
            "operator_id = #{userId}, " +
            "operator_source = 2, " +
            "updated_at = NOW() " +
            "WHERE coach_slot_record_id = #{recordId} " +
            "AND status = 1")
    int updateLockIfAvailable(
            @Param("recordId") Long recordId,
            @Param("newStatus") Integer newStatus,
            @Param("userId") Long userId,
            @Param("lockedUntil") LocalDateTime lockedUntil
    );
}
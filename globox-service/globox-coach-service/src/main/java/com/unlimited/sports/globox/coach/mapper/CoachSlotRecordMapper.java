package com.unlimited.sports.globox.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.coach.entity.CoachSlotRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
     * 根据模板ID和日期查询记录
     */
    @Select("SELECT * FROM coach_slot_record " +
            "WHERE coach_slot_template_id = #{templateId} " +
            "AND booking_date = #{date} " +
            "LIMIT 1")
    CoachSlotRecord selectByTemplateIdAndDate(
            @Param("templateId") Long templateId,
            @Param("date") LocalDate date
    );

    /**
     * 查询指定日期范围内的记录
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
     * 原子性地更新时段状态为锁定（只有当前状态为AVAILABLE时才能更新）
     * 返回受影响的行数，0表示该时段已被其他用户占用
     *
     * @param recordId 记录ID
     * @param newStatus 新状态
     * @param userId 操作人ID
     * @param lockedUntil 锁定截止时间
     * @return 受影响的行数
     */
    @Update("UPDATE coach_slot_record " +
            "SET status = #{newStatus}, " +
            "    locked_by_user_id = #{userId}, " +
            "    locked_until = #{lockedUntil}, " +
            "    locked_type = 1, " +
            "    operator_id = #{userId}, " +
            "    operator_source = 2, " +
            "    updated_at = NOW() " +
            "WHERE coach_slot_record_id = #{recordId} " +
            "AND (status = 1 OR (status = 1 AND locked_by_user_id = #{userId}))")
    int updateLockIfAvailable(
            @Param("recordId") Long recordId,
            @Param("newStatus") Integer newStatus,
            @Param("userId") Long userId,
            @Param("lockedUntil") LocalDateTime lockedUntil
    );
}
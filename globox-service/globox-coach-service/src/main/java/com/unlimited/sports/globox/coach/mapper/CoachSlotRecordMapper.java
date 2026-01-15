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
     * 原子性更新锁定状态（仅当可用时）
     * 使用CAS机制，防止超售
     *
     * 状态说明：
     * 0=AVAILABLE(可用), 1=LOCKED(锁定), 2=UNAVAILABLE(不可预约), 3=CUSTOM_EVENT(自定义日程)
     *
     * @param recordId 记录ID
     * @param status 目标状态（通常为1=LOCKED）
     * @param userId 用户ID
     * @param lockedUntil 锁定截止时间
     * @return 更新的行数，0表示更新失败（记录已被占用或不是可用状态）
     */
    @Update("UPDATE coach_slot_record " +
            "SET status = #{status}, " +
            "    locked_by_user_id = #{userId}, " +
            "    locked_until = #{lockedUntil}, " +
            "    locked_type = 1, " +  // 1=用户下单锁定
            "    updated_at = NOW() " +
            "WHERE coach_slot_record_id = #{recordId} " +
            "AND status = 0")  // 只有状态为0(AVAILABLE)才能更新
    int updateLockIfAvailable(@Param("recordId") Long recordId,
                              @Param("status") Integer status,
                              @Param("userId") Long userId,
                              @Param("lockedUntil") LocalDateTime lockedUntil);

    /**
     * 批量解锁过期的时段
     *
     * @param now 当前时间
     * @return 更新的行数
     */
    @Update("UPDATE coach_slot_record " +
            "SET status = 0, " +  // 0=AVAILABLE
            "    locked_by_user_id = NULL, " +
            "    locked_until = NULL, " +
            "    locked_type = NULL, " +
            "    updated_at = NOW() " +
            "WHERE status = 1 " +  // 1=LOCKED
            "AND locked_until < #{now}")
    int unlockExpiredSlots(@Param("now") LocalDateTime now);
}
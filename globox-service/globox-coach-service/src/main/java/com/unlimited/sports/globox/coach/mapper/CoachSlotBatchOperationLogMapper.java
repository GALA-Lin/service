package com.unlimited.sports.globox.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.coach.entity.CoachSlotBatchOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @since 2026/1/3 14:39
 *
 */
@Mapper
public interface CoachSlotBatchOperationLogMapper extends BaseMapper<CoachSlotBatchOperationLog> {

    /**
     * 查询教练的操作日志
     */
    @Select("SELECT * FROM coach_slot_batch_operation_log " +
            "WHERE coach_user_id = #{coachUserId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<CoachSlotBatchOperationLog> selectRecentLogs(
            @Param("coachUserId") Long coachUserId,
            @Param("limit") int limit
    );
}

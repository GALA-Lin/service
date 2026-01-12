package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.merchant.entity.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.merchant.vo.LockedSlotVo;
import com.unlimited.sports.globox.model.merchant.vo.VenueBookingSlotVo;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 时间槽位记录Mapper
 * @since 2025-12-27
 */
@Mapper
public interface MerchantVenueBookingSlotRecordMapper extends BaseMapper<VenueBookingSlotRecord> {

    /**
     * 根据模板ID和日期查询记录
     */
    VenueBookingSlotRecord selectByTemplateAndDate(
            @Param("templateId") Long templateId,
            @Param("date") LocalDate date
    );

    /**
     * 根据模板ID列表和日期批量查询
     */
    List<VenueBookingSlotRecord> selectByTemplateIdsAndDate(
            @Param("templateIds") List<Long> templateIds,
            @Param("date") LocalDate date
    );

    /**
     * 批量插入记录
     */
    int batchInsert(@Param("records") List<VenueBookingSlotRecord> records);

    /**
     * 统计某日期某场地的记录数
     */
    Integer countByCourtAndDate(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date
    );

    /**
     * 删除未来某个日期范围的可用记录（用于重新生成）
     */
    int deleteAvailableByTemplateAndDateRange(
            @Param("templateId") Long templateId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 根据ID更新状态
     */
    int updateStatusById(
            @Param("recordId") Long recordId,
            @Param("status") Integer status,
            @Param("orderId") String orderId,
            @Param("operatorId") Long operatorId  // 注意：实际对应数据库的operator_id字段
    );

    /**
     * 查询锁定的时段（按场地）
     */
    List<LockedSlotVo> selectLockedSlotsByCourtAndDateRange(
            @Param("courtId") Long courtId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 查询锁定的时段（按场馆）
     */
    List<LockedSlotVo> selectLockedSlotsByVenueAndDateRange(
            @Param("venueId") Long venueId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 根据订单ID查询相关的时段记录
     */
    List<VenueBookingSlotVo> selectSlotsByOrderId(@Param("orderId") String orderId);

    /**
     * 根据场地和日期查询所有记录（包含详细信息）
     */
    List<VenueBookingSlotVo> selectByCourtAndDate(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date
    );

    /**
     * 批量更新记录状态（用于订单操作）
     */
    int batchUpdateStatus(
            @Param("recordIds") List<Long> recordIds,
            @Param("status") Integer status,
            @Param("orderId") String orderId,
            @Param("operatorId") Long operatorId
    );

    /**
     * 检查时段是否可用（避免冲突）
     */
    Integer checkSlotConflict(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date,
            @Param("templateIds") List<Long> templateIds,
            @Param("excludeRecordId") Long excludeRecordId
    );

    /**
     * 查询用户的预订记录
     */
    List<VenueBookingSlotVo> selectByUserId(
            @Param("operatorId") Long operatorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") Integer status
    );

    /**
     * 统计场地某天各状态的时段数量
     */
    List<StatusCountVo> countByCourtAndDateGroupByStatus(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date
    );

    List<VenueBookingSlotRecord> MerchantSelectByCourtIdsAndDate(
            @Param("courtIds") List<Long> courtIds,
            @Param("bookingDate") LocalDate bookingDate
    );
    /**
     * 状态统计VO（内部类）
     */
    @Data
    class StatusCountVo {
        private Integer status;
        private Integer count;
    }
}
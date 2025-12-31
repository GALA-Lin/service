package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.merchant.entity.VenueBookingSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * @Author: Linsen Hu
 * @Date: 2025-12-18-14:39
 * @Description:
 */
@Mapper
public interface BookingSlotMapper extends BaseMapper<VenueBookingSlot> {

    /**
     * 查询场地在指定日期的所有时段
     * @param courtId 场地id
     * @param bookingDate 预定日期
     * @return 时段列表
     */
    List<VenueBookingSlot> selectByCourtAndDate(
            @Param("courtId") Long courtId,
            @Param("bookingDate") LocalDate bookingDate
    );

    /**
     * 查询订单的所有时段
     * @param orderId 订单ID
     * @return 订单的所有时段列表
     */
    List<VenueBookingSlot> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 批量查询时段信息
     * @param slotIds 时段ID列表
     * @return 时段列表
     */
    List<VenueBookingSlot> selectByIds(@Param("slotIds") List<Long> slotIds);

    /**
     * 检查时段是否可用
     * @param courtId 场地ID
     * @param bookingDate 预定日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param excludeSlotId 排除时段ID
     * @return 可用时段数量
     */
    Integer checkSlotAvailable(
            @Param("courtId") Long courtId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeSlotId") Long excludeSlotId
    );

    /**
     * 查询场地在指定日期范围内的所有时段
     * @param venueId 场馆ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 时段列表
     */
    List<VenueBookingSlot> selectByVenueAndDateRange(
            @Param("venueId") Long venueId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 统计场地某天已支付的时段数量
     * @param courtId 场地ID
     * @param bookingDate 预定日期
     * @return 已支付的时段数量
     */
    Integer countPaidSlotsByCourtAndDate(
            @Param("courtId") Long courtId,
            @Param("bookingDate") LocalDate bookingDate
    );


    /**
     * 统计场地在指定日期的时段数量
     * @param courtId 场地ID
     * @param bookingDate 预定日期
     * @return 时段数量
     */
    Integer countByCourtAndDate(
            @Param("courtId") Long courtId,
            @Param("bookingDate") LocalDate bookingDate
    );

    /**
     * 更改场地状态时，某天所有可删除的时段（状态为BOOKABLE的）
     * 用于重新配置时间段时只覆盖未预定的
     * @param courtId 场地ID
     * @param bookingDate 预定日期
     * @return 可删除的时段数量
     */
    Integer deleteAvailableSlotsByCourtAndDate(
            @Param("courtId") Long courtId,
            @Param("bookingDate") LocalDate bookingDate
    );


    /**
     * 使用行锁查询槽位（SELECT ... FOR UPDATE）
     * 用于RPC价格查询时锁定槽位，防止并发修改
     *
     * @param slotIds 槽位ID列表
     * @param bookingDate 预定日期
     * @return 被锁定的槽位列表
     */
    @Select("<script>" +
            "SELECT * FROM venue_booking_slot " +
            "WHERE booking_slot_id IN " +
            "<foreach item='id' index='index' collection='slotIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND booking_date = #{bookingDate} " +
            "FOR UPDATE" +
            "</script>")
    List<VenueBookingSlot> selectWithLockByIds(
            @Param("slotIds") List<Long> slotIds,
            @Param("bookingDate") LocalDate bookingDate
    );

    /**
     * 批量查询订单的所有时段
     * @param orderIds 订单ID列表
     * @return 时段列表
     */
    @Select("<script>" +
            "SELECT * FROM venue_booking_slot " +
            "WHERE order_id IN " +
            "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " ORDER BY booking_date, start_time" +
            "</script>")
    List<VenueBookingSlot> selectByOrderIds(@Param("orderIds") List<Long> orderIds);}

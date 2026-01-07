package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.dubbo.merchant.dto.PricingRequestDto;
import com.unlimited.sports.globox.dubbo.merchant.dto.PricingResultDto;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.venue.dto.*;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.vo.CourtSlotVo;
import com.unlimited.sports.globox.model.venue.vo.VenueSnapshotVo;
import com.unlimited.sports.globox.venue.dto.ActivityPreviewContext;
import com.unlimited.sports.globox.venue.dto.SlotBookingContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IBookingService {



    /**
     * 获取场馆指定日期所有场地的槽位占用情况
     *
     * @param dto 查询条件，包含场馆ID和预订日期
     * @return 场地列表，每个场地包含其所有时间槽位的占用状态
     */
    List<CourtSlotVo> getCourtSlots(GetCourtSlotsDto dto);

    /**
     * 预订预览 - 计算普通槽位价格但不锁定槽位
     *
     * @param userId 用户ID
     * @param dto 预订请求参数
     * @return 包含场馆信息和价格计算结果
     */
    BookingPreviewResponseVo previewGeneralBooking(Long userId, GeneralBookingPreviewRequestDto dto);

    /**
     * 活动预览 - 获取活动信息和价格但不锁定活动
     *
     * @param userId 用户ID
     * @param dto 活动预览请求参数
     * @return 包含场馆信息和活动价格信息
     */
    BookingPreviewResponseVo previewActivity(Long userId, ActivityBookingPreviewRequestDto dto);

    /**
     * 验证并准备槽位预订上下文信息
     * 公共方法，供RPC服务和预览服务复用
     *
     * 包括以下步骤：
     * 1. 查询槽位模板并验证存在性
     * 2. 验证槽位可用性
     * 3. 查询场地信息并验证
     * 4. 验证所有槽位来自同一场馆
     * 5. 查询场馆信息
     * 6. 构建场地名称映射和场地对象映射
     *
     * @param slotIds 槽位ID列表
     * @param bookingDate 预订日期
     * @param userId 用户ID（用于日志）
     * @return 槽位预订上下文信息
     * @throws com.unlimited.sports.globox.common.exception.GloboxApplicationException 如果验证失败
     */
    SlotBookingContext validateAndPrepareBookingContext(List<Long> slotIds, LocalDate bookingDate, Long userId);

    /**
     * 验证并准备活动预览上下文信息
     * 公共方法，供RPC服务和预览服务复用
     *
     * 包括以下步骤：
     * 1. 查询活动信息
     * 2. 验证活动存在性
     * 3. 验证活动是否有效（报名期限未到期）
     * 4. 查询场馆信息并验证
     * 5. 查询场地信息并验证
     * 6. 查询活动类型信息
     *
     * @param activityId 活动ID
     * @return 活动预览上下文信息
     * @throws com.unlimited.sports.globox.common.exception.GloboxApplicationException 如果验证失败
     */
    ActivityPreviewContext validateAndPrepareActivityContext(Long activityId);

    /**
     * 获取场馆快照信息
     * 公共方法，供RPC服务和内部服务复用

     * @param userLatitude 用户纬度
     * @param userLongitude 用户经度
     * @return 场馆快照信息（包含距离计算、封面图、便利设施等）
     */
    VenueSnapshotVo getVenueSnapshotVo( Double userLatitude, Double userLongitude,Venue venue);



    /**
     * 在分布式锁保护下执行事务性的预订和计价逻辑
     * 此方法会开启事务，事务快照在获取锁之后建立，确保能读取到其他已提交事务的最新数据
     *
     * @param dto 价格请求DTO
     * @param templates 槽位模板列表
     * @param venue 场馆对象（外部已查询）
     * @return 价格结果DTO
     */
    PricingResultDto executeBookingInTransaction(
            PricingRequestDto dto,
            List<VenueBookingSlotTemplate> templates,
            Venue venue,
            Map<Long, String> courtNameMap);

}

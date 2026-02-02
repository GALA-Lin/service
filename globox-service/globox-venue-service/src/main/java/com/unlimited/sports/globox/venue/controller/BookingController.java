package com.unlimited.sports.globox.venue.controller;


import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;

import com.unlimited.sports.globox.common.utils.RequestContextHolder;
import com.unlimited.sports.globox.common.utils.ServletUtils;
import com.unlimited.sports.globox.model.venue.dto.*;
import com.unlimited.sports.globox.model.venue.vo.CourtSlotVo;
import com.unlimited.sports.globox.venue.service.IBookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/venue/booking")
public class BookingController {

    @Autowired
    private IBookingService bookingService;


    /**
     * 获取场地的时间槽位列表（预订槽位）
     *
     * @param venueId 场馆ID
     * @return 场地及其时间槽位列表
     */
    @GetMapping("/{venueId}/courts/slots")
    public R<List<CourtSlotVo>> getCourtSlots(@PathVariable Long venueId, @Valid GetCourtSlotsDto dto) {
        dto.setVenueId(venueId);
        Long userId = ServletUtils.getHeaderValue(RequestHeaderConstants.HEADER_USER_ID, Long.class);
        if(userId == null) {
            userId = 1L;
        }
        dto.setUserId(userId);
        List<CourtSlotVo> result = bookingService.getCourtSlots(dto);
        return R.ok(result);
    }


    /**
     * 预订预览 - 获取订单信息和价格(普通槽位)
     *
     * @param dto 预订请求参数
     * @return 包含场馆信息和价格计算结果
     */
    @PostMapping("/preview/general")
    public R<BookingPreviewResponseVo> previewGeneralBooking(@Valid @RequestBody GeneralBookingPreviewRequestDto dto) {
        // 从token中获取用户ID
        Long userId = RequestContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        if (userId == null) {
            log.warn("未获取到用户ID，无法预览订单");
            throw new GloboxApplicationException("未获取到用户ID，无法预览订单");
        }
        log.info("收到预订普通槽位预览请求 - userId: {}, slotIds: {}, bookingDate: {}",
                userId, dto.getSlotIds(), dto.getBookingDate());

        BookingPreviewResponseVo result = bookingService.previewGeneralBooking(userId, dto);
        return R.ok(result);
    }


    /**
     * 预订预览 - 获取订单信息和价格(活动槽位)
     *
     * @param dto 预订请求参数
     * @return 包含场馆信息和价格计算结果
     */
    @PostMapping("/preview/activity")
    public R<BookingPreviewResponseVo> previewActivityBooking(@Valid @RequestBody ActivityBookingPreviewRequestDto dto) {
        Long userId = RequestContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        if (userId == null) {
            log.warn("未获取到用户ID，无法预览订单");
            throw new GloboxApplicationException("未获取到用户ID，无法预览订单");
        }
        log.info("收到预订活动槽位预览请求 - userId: {}, activityId: {}, bookingDate: {}",
                userId, dto.getActivityId(), dto.getBookingDate());
        return R.ok(bookingService.previewActivity(userId,dto));

    }



}

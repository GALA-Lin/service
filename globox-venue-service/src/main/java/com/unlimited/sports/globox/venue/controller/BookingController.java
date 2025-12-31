package com.unlimited.sports.globox.venue.controller;


import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.result.R;

import com.unlimited.sports.globox.common.utils.ServletUtils;
import com.unlimited.sports.globox.model.venue.dto.*;
import com.unlimited.sports.globox.model.venue.vo.CourtSlotVo;
import com.unlimited.sports.globox.venue.service.IBookingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/venue/booking")
public class BookingController {
    ;
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



}

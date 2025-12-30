package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.model.venue.dto.GetCourtSlotsDto;
import com.unlimited.sports.globox.model.venue.vo.CourtSlotVo;

import java.util.List;

public interface IBookingService {



    /**
     * 获取场馆指定日期所有场地的槽位占用情况
     *
     * @param dto 查询条件，包含场馆ID和预订日期
     * @return 场地列表，每个场地包含其所有时间槽位的占用状态
     */
    List<CourtSlotVo> getCourtSlots(GetCourtSlotsDto dto);
}

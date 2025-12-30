package com.unlimited.sports.globox.model.venue.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

/**
 * 场地槽位VO
 */
@Data
@Builder
public class CourtSlotVo {

    @NonNull
    private Long courtId;

    @NonNull
    private String courtName;

    @NonNull
    private Integer courtType;

    @NonNull
    private String courtTypeDesc;

      
    private Integer groundType;

      
    private String groundTypeDesc;

    @NonNull
    private List<BookingSlotVo> slots;
}

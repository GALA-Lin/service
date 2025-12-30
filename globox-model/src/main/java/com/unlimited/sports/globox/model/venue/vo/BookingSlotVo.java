package com.unlimited.sports.globox.model.venue.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 预订槽位VO
 */
@Data
@Builder
public class BookingSlotVo {

    private Long bookingSlotId;

    /**
     * 槽位是按日期搜索的,这里只需要时间部分,使用LocalTime即可,具体日期会在外部指定
     */
    @NonNull
    private LocalTime startTime;

      
    private LocalTime endTime;

    @NonNull
    private Integer status;

    @NonNull
    private String statusDesc;

    @NonNull
    private Boolean isAvailable;

    /**
     * 槽位价格（根据日期类型计算：工作日/周末/节假日）
     */
    @NonNull
    private BigDecimal price;
}

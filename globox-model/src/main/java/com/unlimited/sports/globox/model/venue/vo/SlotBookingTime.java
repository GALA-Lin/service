package com.unlimited.sports.globox.model.venue.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * 槽时间信息
 * 公共VO，用于订单预览和订单详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotBookingTime {

    /**
     * 开始时间
     */
    @NotNull
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @NotNull
    private LocalTime endTime;
}

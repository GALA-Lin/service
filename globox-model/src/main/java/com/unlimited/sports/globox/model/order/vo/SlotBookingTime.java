package com.unlimited.sports.globox.model.order.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * 槽时间信息 - 响应载体类
 */
@Data
public class SlotBookingTime {
    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;
}

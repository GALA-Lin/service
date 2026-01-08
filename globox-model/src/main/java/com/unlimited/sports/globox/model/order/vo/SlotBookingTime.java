package com.unlimited.sports.globox.model.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * 槽时间信息 - 响应载体类
 */
@Data
@Schema(name = "SlotBookingTime", description = "预订时间段")
public class SlotBookingTime {

    @NotNull
    @Schema(description = "开始时间", example = "09:00")
    private LocalTime startTime;

    @NotNull
    @Schema(description = "结束时间", example = "09:30")
    private LocalTime endTime;
}

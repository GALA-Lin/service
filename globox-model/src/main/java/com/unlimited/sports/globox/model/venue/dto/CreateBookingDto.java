package com.unlimited.sports.globox.model.venue.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 创建预订请求DTO
 */
@Data
public class CreateBookingDto {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "场地ID不能为空")
    private Long courtId;

    @NotNull(message = "预订日期不能为空")
    private LocalDate bookingDate;

    @NotEmpty(message = "预订时间段不能为空")
    private List<LocalTime> startTimes;
}

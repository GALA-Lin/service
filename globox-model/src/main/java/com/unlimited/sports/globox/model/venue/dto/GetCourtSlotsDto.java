package com.unlimited.sports.globox.model.venue.dto;


import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 获取场馆场地槽位请求DTO
 */
@Data
public class GetCourtSlotsDto {

    private Long venueId;

    @NotNull(message = "预订日期不能为空")
    private LocalDate bookingDate;

    private Long userId;
}

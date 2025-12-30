package com.unlimited.sports.globox.model.order.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建订单dto
 */
@Data
public class CreateVenueOrderDto {
    @NotNull(message = "预订日期不能为空")
    private LocalDate bookingDate;

    @NotNull(message = "预定的场地不能为空")
    @Size(min = 1, message = "预定的场地不能为空")
    private List<Long> slotIds;
}

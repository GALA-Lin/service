package com.unlimited.sports.globox.model.venue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 查询不可预订Away球场的DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryUnavailableAwayVenuesDto {

    /**
     * 查询日期
     */
    @NotNull(message = "日期不能为空")
    private LocalDate date;

    /**
     * 查询开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    /**
     * 查询结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;
}

package com.unlimited.sports.globox.model.merchant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.LocalDate;

/**
 * @since 2025/12/29 17:42
 * 时段锁定请求
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlotLockRequestDto {
    /**
     * 时段模板ID
     */
    @NonNull
    private Long templateId;

    /**
     * 预订日期
     */
    @NonNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;
}
package com.unlimited.sports.globox.model.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @since 2026/1/26 16:37
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRefundDto implements Serializable {


    @NotNull
    private Long venueId;

    @NotNull
    private Long orderNo;

    @NotNull
    private String remark;
}
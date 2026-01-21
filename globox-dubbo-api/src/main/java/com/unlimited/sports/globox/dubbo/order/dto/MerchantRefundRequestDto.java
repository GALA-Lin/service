package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 商家申请退款 dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRefundRequestDto implements Serializable {

    @NotNull
    private Long merchantId;

    @NotNull
    private Long venueId;

    @NotNull
    private Long orderNo;

    @NotNull
    private String remark;
}

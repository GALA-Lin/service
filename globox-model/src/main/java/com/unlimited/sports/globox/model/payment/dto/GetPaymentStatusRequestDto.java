package com.unlimited.sports.globox.model.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 获取支付状态 dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetPaymentStatusRequestDto implements Serializable {

    @NotNull(message = "支付端类型不能为空")
    private Integer paymentTypeCode;

}

package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 商家确认订单 rpc - 请求类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantConfirmRequestDto {

    @NotNull
    private Long orderNo;

    /**
     * 是否自动确认
     */
    @NotNull
    private boolean isAutoConfirm;


    /**
     * 如果不是自动确认，传入商家 id
     */
    private Long merchantId;

}

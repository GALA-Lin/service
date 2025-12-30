package com.unlimited.sports.globox.model.venue.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 获取订单详情请求DTO
 */
@Data
public class GetOrderDetailDto {

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 用户纬度（用于计算距离）
     */
    private BigDecimal userLatitude;

    /**
     * 用户经度（用于计算距离）
     */
    private BigDecimal userLongitude;
}

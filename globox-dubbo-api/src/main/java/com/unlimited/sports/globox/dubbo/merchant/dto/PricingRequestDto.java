package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建订单前查询价格 - 请求 DTO
 *
 * @author dk
 * @since 2025/12/22 17:50
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PricingRequestDto implements Serializable {

    /**
     * 用户 ID
     */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /**
     * 预定日期
     */
    @NotNull(message = "预定日期不能为空")
    private LocalDate bookingDate;

    /**
     * 预定的槽列表
     */
    @NotNull(message = "预定的时段不能为空")
    private List<Long> slotIds;

    /**
     * 用户下订单的时候填写的手机号
     */
    @NotNull(message = "手机号不能为空")
    private String userPhone;
}

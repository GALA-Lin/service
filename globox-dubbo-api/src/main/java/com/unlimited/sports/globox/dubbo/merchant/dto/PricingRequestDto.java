package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    /**
     * 用户选择的订单级附加费用ID列表（可选，不传视为默认不选）
     */
    private List<Long> selectedOrderExtraIds;

    /**
     * 用户选择的每个槽位附加费用ID列表（key=slotTemplateId,即槽位id,表示为该槽位选择的订单项级别附加费id列表
     * 可不传,默认不选）
     */
    private Map<Long, List<Long>> selectedItemExtraBySlotId;
}

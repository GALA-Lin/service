package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 教练服务获取订单详情 - 请求 dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachGetOrderDetailsRequestDto implements Serializable {
    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    @NotNull(message = "教练 ID 不能为空")
    private Long coachId;
}

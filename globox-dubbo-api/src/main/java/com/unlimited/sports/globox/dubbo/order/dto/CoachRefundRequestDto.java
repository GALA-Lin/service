package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 教练申请退款 dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachRefundRequestDto implements Serializable {
    @NotNull
    private Long coachId;

    @NotNull
    private Long orderNo;

    @NotNull
    private String remark;
}

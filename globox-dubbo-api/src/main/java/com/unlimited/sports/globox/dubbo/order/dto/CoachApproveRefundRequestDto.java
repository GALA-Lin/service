package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dubbo.common.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 教练审批退款请求的数据传输对象。
 * 该类用于封装教练处理退款申请时所需的信息，包括订单号、退款申请ID、教练ID和退款比例等。
 * 可选的商家备注字段允许教练在处理退款时添加额外的说明信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachApproveRefundRequestDto implements Serializable {
    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    @NotNull(message = "退款申请ID不能为空")
    private Long refundApplyId;

    @NotNull(message = "教练ID不能为空")
    private Long coachId;

    @NotNull(message = "退款比例不能为空")
    private BigDecimal refundPercentage;

    /**
     * 可选：商家备注
     */
    @Nullable
    private String remark;
}

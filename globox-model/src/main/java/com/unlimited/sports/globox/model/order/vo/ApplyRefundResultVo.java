package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款申请结果的值对象。
 * 包含了订单退款申请的相关信息，如订单号、退款申请ID、是否为整单退款等。
 */
@Data
@Builder
@Schema(name = "ApplyRefundResultVo", description = "退款申请结果返回对象")
public class ApplyRefundResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    @NotNull
    @Schema(description = "订单号", example = "202512180001")
    private Long orderNo;

    /**
     * 退款申请ID
     */
    @Schema(description = "退款申请ID", example = "10086")
    private Long refundApplyId;

    /**
     * 是否可退款
     */
    @NotNull
    @Schema(description = "是否可退款", example = "true")
    private boolean isRefundable;

    /**
     * 当前退款申请状态
     */
    @Schema(description = "退款申请状态", example = "APPLIED")
    private ApplyRefundStatusEnum applyStatus;

    /**
     * 申请时间
     */
    @NotNull
    @Schema(description = "退款申请时间", example = "2025-12-18T14:30:00")
    private LocalDateTime appliedAt;

    /**
     * 不可退款时的原因
     */
    @Schema(description = "不可退款时的原因", example = "存在已过最晚可退时间的退款项")
    private String reason;

}

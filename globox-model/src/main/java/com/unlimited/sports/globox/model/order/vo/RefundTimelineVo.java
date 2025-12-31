package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.OperatorTypeEnum;
import com.unlimited.sports.globox.common.enums.order.OrderActionEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款处理时间线 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RefundTimelineVo", description = "退款处理时间线节点")
public class RefundTimelineVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "操作动作类型", example = "REFUND_APPLY")
    private OrderActionEnum action;

    @NotNull
    @Schema(description = "操作动作名称", example = "提交退款申请")
    private String actionName;

    @NotNull
    @Schema(description = "操作发生时间", example = "2025-12-18T14:30:00")
    private LocalDateTime at;

    @Null
    @Schema(description = "操作备注说明", example = "用户发起退款申请", nullable = true)
    private String remark;

    /**
     * 操作人信息
     */
    @NotNull
    @Schema(description = "操作人类型", example = "USER")
    private OperatorTypeEnum operatorType;

    @NotNull
    @Schema(description = "操作人ID", example = "10001")
    private Long operatorId;

    @NotNull
    @Schema(description = "操作人名称", example = "张三")
    private String operatorName;
}
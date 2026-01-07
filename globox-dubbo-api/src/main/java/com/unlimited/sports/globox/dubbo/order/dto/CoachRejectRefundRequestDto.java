package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dubbo.common.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 用于封装教练拒绝退款请求的数据传输对象。
 * 此类包含订单号、退款申请ID和教练ID等必填字段，以及一个可选的备注字段，用于记录拒绝原因。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachRejectRefundRequestDto implements Serializable {
    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    @NotNull(message = "退款申请ID不能为空")
    private Long refundApplyId;

    @NotNull(message = "教练ID不能为空")
    private Long coachId;

    /**
     * 可选：拒绝原因备注（给用户/运营看）
     */
    @Nullable
    private String remark;
}

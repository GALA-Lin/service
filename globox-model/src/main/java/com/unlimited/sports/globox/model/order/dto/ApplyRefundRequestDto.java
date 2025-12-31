package com.unlimited.sports.globox.model.order.dto;

import com.unlimited.sports.globox.common.enums.order.UserRefundReasonEnum;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(name = "ApplyRefundRequestDto", description = "用户申请退款请求参数")
public class ApplyRefundRequestDto implements Serializable {

    @Schema(description = "订单号",
            example = "130341221716594688",
            required = true)
    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    @Schema(description = "需要退款的订单项ID列表",
            example = "[10001,10002]",
            required = true)
    @NotNull(message = "需要退款的订单项不能为空")
    @Size(min = 1, message = "需要退款的订单项不能为空")
    private List<Long> orderItemIds;

    @Schema(description = """
                    退款原因编码：
                    1=改变主意
                    2=日程冲突
                    3=场馆问题
                    4=重复付款
                    5=质量问题
                    6=天气原因
                    7=个人紧急事件
                    8=其他
                    """,
            example = "2",
            required = true,
            allowableValues = {
                    "1","2","3","4","5","6","7","8"
            })
    @NotNull(message = "退款原因不能为空")
    private UserRefundReasonEnum reasonCode;

    @Schema(description = "退款原因补充说明（reasonCode=8 时建议填写）",
            example = "临时有事无法到场",
            nullable = true)
    private String reasonDetail;
}
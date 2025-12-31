package com.unlimited.sports.globox.model.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 取消订单返回结果
 */
@Data
@Builder
@Schema(
        name = "CancelOrderResultVo",
        description = "订单取消结果返回对象"
)
public class CancelOrderResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    @NotNull
    @Schema(description = "订单号", example = "202512180001")
    private Long orderNo;

    /**
     * 当前订单状态
     */
    @NotNull
    @Schema(description = "当前订单状态码", example = "40")
    private Integer orderStatus;

    /**
     * 订单状态描述
     */
    @NotNull
    @Schema(description = "订单状态描述", example = "已取消")
    private String orderStatusName;

    /**
     * 取消时间
     * 若订单未真正取消，该字段可能为空
     */
    @Null
    @Schema(description = "订单取消时间（若未取消则为空）",
            example = "2025-12-18T16:45:00",
            nullable = true)
    private LocalDateTime cancelledAt;
}
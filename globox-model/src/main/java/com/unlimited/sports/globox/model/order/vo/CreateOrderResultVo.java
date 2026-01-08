package com.unlimited.sports.globox.model.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 创建订单结果
 *
 * @author dk
 * @since 2025/12/22 09:55
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateOrderResultVo", description = "创建订单结果返回对象")
public class CreateOrderResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    @NotNull
    @Schema(description = "订单号", example = "202512180001")
    private Long orderNo;
}
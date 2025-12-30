package com.unlimited.sports.globox.model.order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 创建订单结果
 *
 * @author dk
 * @since 2025/12/22 09:55
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResultVo {

    /**
     * 订单号
     */
    @NotNull
    private Long orderNo;
}

package com.unlimited.sports.globox.model.demo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单 - 请求参数 - 测试 seata
 *
 * @author dk
 * @since 2025/12/20 08:50
 */
@Data
public class CreateVenueOrderRequestVO {
    private Long userId;
    private BigDecimal money;
}

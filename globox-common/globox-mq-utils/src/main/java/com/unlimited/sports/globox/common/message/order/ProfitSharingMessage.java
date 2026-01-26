package com.unlimited.sports.globox.common.message.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 请求分账 message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitSharingMessage implements Serializable {
    private Long orderNo;

    private String outTradeNo;

    private String tradeNo;
}

package com.unlimited.sports.globox.common.message.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 订单自动关闭 message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAutoCancelMessage implements Serializable {
    private Long orderNo;
    private Long userId;
    private LocalDate bookingDate;
    private List<Long> slotIds;
}

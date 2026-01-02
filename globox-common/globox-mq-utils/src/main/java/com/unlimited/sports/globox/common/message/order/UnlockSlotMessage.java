package com.unlimited.sports.globox.common.message.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 取消锁场 message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnlockSlotMessage implements Serializable {
    private Long userId;
    private List<Long> recordIds;
    private LocalDate bookingDate;
}

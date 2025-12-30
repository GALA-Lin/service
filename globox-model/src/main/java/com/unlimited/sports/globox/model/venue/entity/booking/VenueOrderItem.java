package com.unlimited.sports.globox.model.venue.entity.booking;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("venue_order_items")
public class VenueOrderItem {

    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;

    private Long orderId;

    private Long bookingSlotId;

    private Long courtId;

    private LocalDate bookingDate;

    private LocalTime startTime;

    private BigDecimal unitPrice;

    private BigDecimal slotExtraCharge;

    private BigDecimal subtotal;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

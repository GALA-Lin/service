package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 获取用户订单列表 - 请求载体类
 * 订场订单：订单编号、场馆名称、场地名称、预约订场日期、订场槽的时间、价格、订单状态
 * 教练订单：订单编号、预约教练日期、教练槽的时间、价格、订单状态
 */
@Data
@Builder
public class GetOrderVo {

    @NotNull
    private Long orderNo;

    @NotNull
    private Integer sellerType;

    @NotNull
    private Long sellerId;

    /**
     * 场馆(教练)名称
     */
    @NotNull
    private String sellerName;

    @NotNull
    private Long resourceId;

    /**
     * 场地（教练）名称
     */
    @NotNull
    private String resourceName;

    /**
     * 预约订场日期
     */
    @NotNull
    private LocalDate bookingDate;

    /**
     * 订单总金额
     */
    @NotNull
    private BigDecimal amount;

    /**
     * 当前订单状态
     */
    @NotNull
    private OrderStatusEnum currentOrderStatus;


    /**
     * 预定的时间段
     */
    @NotNull
    private List<SlotBookingTime> slotBookingTimes;
}

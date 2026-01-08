package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 获取用户订单列表 - 请求载体类
 * 订场订单：订单编号、场馆名称、场地名称、预约订场日期、订场槽的时间、价格、订单状态
 * 教练订单：订单编号、预约教练日期、教练槽的时间、价格、订单状态
 */
@Data
@Builder
@Schema(name = "GetOrderVo", description = "订单列表/简要订单信息返回对象")
public class GetOrderVo {

    @NotNull
    @Schema(description = "订单号", example = "202512180001")
    private Long orderNo;

    @NotNull
    @Schema(description = "卖方类型（场馆 / 教练）", example = "1=场馆 2=教练")
    private Integer sellerType;

    @NotNull
    @Schema(description = "卖方ID（场馆ID / 教练ID）", example = "10")
    private Long sellerId;

    /**
     * 场馆(教练)名称
     */
    @NotNull
    @Schema(description = "卖方名称（场馆名 / 教练名）", example = "星耀网球中心")
    private String sellerName;

    // TODO ETA 2025/01/03 修改结构，将其变为 List
    @NotNull
    @Schema(description = "资源ID（当前为单值，后续将调整为列表）", example = "101")
    private Long resourceId;

    /**
     * 场地（教练）名称
     */
    @NotNull
    @Schema(description = "资源名称（场地名 / 教练名）", example = "1号场")
    private String resourceName;

    /**
     * 预约订场日期
     */
    @NotNull
    @Schema(description = "预约日期", example = "2025-12-20")
    private LocalDate bookingDate;


    /**
     * 是否活动订单
     */
    @NotNull
    @Schema(description = "是否活动订单", example = "false")
    private boolean isActivity;


    /**
     * 活动类型名称
     */
    @Schema(description = "活动类型名称", example = "畅打")
    private String activityTypeName;

    /**
     * 订单总金额
     */
    @NotNull
    @Schema(description = "订单总金额", example = "320.00")
    private BigDecimal amount;

    @NotNull
    @Schema(description = "订单创建时间")
    private LocalDateTime createdAt;

    /**
     * 当前订单状态
     */
    @NotNull
    @Schema(description = "当前订单状态", example = "1")
    private OrderStatusEnum currentOrderStatus;

    /**
     * 预定的时间段
     */
    @NotNull
    @Schema(description = "预订时间段列表")
    private List<SlotBookingTime> slotBookingTimes;
}
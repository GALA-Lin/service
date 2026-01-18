package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.ChargeModeEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.SellerTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.ScriptAssert;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 订单详情 - 响应载体类
 */
@Data
@Builder
@Schema(name = "GetOrderDetailsVo", description = "订单详情返回对象")
public class GetOrderDetailsVo {

    @NotNull
    @Schema(description = "订单号", example = "202512180001")
    private Long orderNo;

    @NotNull
    @Schema(description = "订单类型（商家 / 教练）", example = "VENUE")
    private SellerTypeEnum orderType;

    @Schema(description = "场馆信息快照")
    private VenueSnapshotVo venueSnapshot;

    @Schema(description = "教练信息快照")
    private CoachSnapshotVo coachSnapshotVo;

    @NotNull
    @Schema(description = "卖方名称（场馆名 / 教练名）", example = "星耀网球中心")
    private String sellerName;

    @NotNull
    @Schema(description = "订单总金额", example = "320.00")
    private BigDecimal amount;

    @NotNull
    @Schema(description = "预订日期", example = "2025-12-20")
    private LocalDate bookingDate;

    @NotNull
    @Schema(description = "当前订单状态", example = "PAID")
    private OrderStatusEnum currentOrderStatus;

    /**
     * 是否可取消
     */
    private Boolean isCancelable;

    /**
     * 当前订单是否可退款
     */
    private Boolean isRefundable;

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

    @NotNull
    @Schema(description = "订单级附加费用列表")
    private List<ExtraChargeVo> orderLevelExtraCharges;

    @NotNull
    @Schema(description = "订单创建时间")
    private LocalDateTime createdAt;

    /**
     * 订单项列表
     */
    @NotNull
    @Schema(description = "订单项列表")
    private List<OrderItemDetailVo> items;


    @Data
    @Builder
    @Schema(description = "订单项明细")
    public static class OrderItemDetailVo {

        @NotNull
        @Schema(description = "订单项ID", example = "10001")
        private Long itemId;

        @Schema(description = "场地信息快照")
        private CourtSnapshotVo courtSnapshot;

        @NotNull
        @Schema(description = "订单项基础金额", example = "150.00")
        private BigDecimal itemBaseAmount;

        /**
         * item.subtotal
         */
        @NotNull
        @Schema(description = "订单项实际金额（含附加费用）", example = "160.00")
        private BigDecimal itemAmount;

        @Schema(description = "订单项附加费用")
        private List<ExtraChargeVo> extraCharges;

        @NotNull
        @Schema(description = "退款状态", example = "NONE")
        private RefundStatusEnum refundStatus;

        /**
         * 预定时间段
         */
        @NotNull
        @Schema(description = "预订时间段列表")
        private List<SlotBookingTime> slotBookingTimes;

        /**
         * 退款信息（没有则为 null）
         */
        @Schema(description = "退款信息（无退款时为空）")
        private ItemRefundVo refund;

        /**
         * 当前订单项是否可退款
         */
        private Boolean isItemRefundable;

        /**
         * 当前订单项可退百分比
         * 例如：100.00 表示100%
         */
        private BigDecimal refundPercentage;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "附加费用信息")
    public static class ExtraChargeVo {

        @NotNull
        @Schema(description = "费用类型ID", example = "1")
        private Long chargeTypeId;

        @NotNull
        @Schema(description = "费用名称", example = "夜场附加费")
        private String chargeName;

        @NotNull
        @Schema(description = "计费模式", example = "FIXED")
        private ChargeModeEnum chargeMode;

        @NotNull
        @Schema(description = "固定值 / 单价", example = "10.00")
        private BigDecimal fixedValue;

        @NotNull
        @Schema(description = "实际收费金额", example = "20.00")
        private BigDecimal chargeAmount;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单项退款信息")
    public static class ItemRefundVo {

        @NotNull
        @Schema(description = "退款金额", example = "120.00")
        private BigDecimal refundAmount;

        @NotNull
        @Schema(description = "退款手续费", example = "5.00")
        private BigDecimal refundFee;

        @NotNull
        @Schema(description = "退款状态", example = "REFUNDED")
        private RefundStatusEnum refundStatus;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "场馆信息快照")
    public static class VenueSnapshotVo {

        @NotNull
        @Schema(description = "场馆ID", example = "1")
        private Long id;

        @NotNull
        @Schema(description = "场馆名称", example = "星耀网球中心")
        private String name;

        @NotNull
        @Schema(description = "联系电话", example = "13800000000")
        private String phone;

        @NotNull
        @Schema(description = "所在区域", example = "上海市浦东新区")
        private String region;

        @NotNull
        @Schema(description = "详细地址", example = "世纪大道100号")
        private String address;

        @NotNull
        @Schema(description = "封面图URL")
        private String coverImage;

        /**
         * 单位 km
         */
        @NotNull
        @Schema(description = "用户到场馆距离（km）", example = "3.5")
        private BigDecimal distance;

        @NotNull
        @Schema(description = "场馆设施列表", example = "[\"停车场\",\"更衣室\"]")
        private List<String> facilities;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "场地信息快照")
    public static class CourtSnapshotVo {

        @NotNull
        @Schema(description = "场地ID", example = "101")
        private Long id;

        @NotNull
        @Schema(description = "场地名称", example = "1号场")
        private String name;

        @NotNull
        @Schema(description = "地面类型", example = "1")
        private Integer groundType;

        @NotNull
        @Schema(description = "场地类型", example = "2")
        private Integer courtType;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "教练信息快照")
    public static class CoachSnapshotVo {
        /**
         * 教练用户ID
         */
        @NotNull
        private Long coachUserId;

        /**
         * 教练姓名
         */
        private String coachName;

        /**
         * 教练头像
         */
        private String coachAvatar;

        /**
         * 教练联系电话
         */
        private String coachPhone;

        /**
         * 常驻服务区域
         */
        private String serviceArea;

        /**
         * 证书等级列表
         */
        private List<String> certificationLevels;

        /**
         * 教学年限
         */
        private Integer teachingYears;

        /**
         * 专长标签
         */
        private List<String> specialtyTags;

        /**
         * 综合评分
         */
        private BigDecimal ratingScore;

        /**
         * 评价数
         */
        private Integer ratingCount;
    }
}
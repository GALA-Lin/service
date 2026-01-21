package com.unlimited.sports.globox.model.merchant.vo;

import com.unlimited.sports.globox.model.merchant.entity.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.merchant.entity.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
import com.unlimited.sports.globox.model.venue.enums.SlotTypeEnum;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 预订时段视图（用于订单详情）
 * @since 2025-12-27
 * 展示订单中包含的具体时段信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueBookingSlotVo {

    /**
     * 槽位ID（通用字段）
     * - slotType=1时：为bookingSlotRecordId或bookingSlotTemplateId（普通槽位ID）
     * - slotType=2时：为activityId（活动ID）
     */
    @NonNull
    private Long bookingSlotId;

    /**
     * 槽位时间（无论普通还是活动都需要）
     * 槽位是按日期搜索的,这里只需要时间部分,使用LocalTime即可,具体日期会在外部指定
     */
    @NonNull
    private LocalTime startTime;

    private LocalTime endTime;

    /**
     * 槽位类型：1=普通槽位(NORMAL)，2=活动槽位(ACTIVITY)
     * 见 {@link com.unlimited.sports.globox.model.venue.enums.SlotTypeEnum}
     */
    @NonNull
    private Integer slotType;

    /**
     * 场地ID
     */
    private Long courtId;

    private String courtName;

    private LocalDate bookingDate;

    private String statusName;

    /**
     * 可取消
     */
    private Boolean cancelable;

    /**
     * 预订用户ID
     */
    private Long userId;


    // ===== 普通槽位字段（slotType=1时使用）=====

    /**
     * 槽位状态：1=AVAILABLE(可预订)，2=LOCKED_IN(已占用)，3=EXPIRED(不可预定)
     */
    private Integer status;

    /**
     * 槽位状态描述
     */
    private String statusDesc;

    /**
     * 是否可预订
     */
    private Boolean isAvailable;

    /**
     * 槽位价格（根据日期类型计算：工作日/周末/节假日）
     */
    private BigDecimal price;

    /**
     * 模板ID（区别于bookingSlotId）
     */
    private Long templateId;

    /**
     * 锁定类型：1=商家锁场，2=用户订单
     */
    private Integer lockedType;

    /**
     * 锁定原因
     */
    private String lockReason;

    /**
     * 关联订单ID（如果有）
     */
    private String orderId;

    /**
     * 是否是本人预定的槽位（仅当slotType=1时使用）
     * true: 该槽位是当前用户预定的
     * false: 该槽位是他人预定的，或未预定
     */
    private Boolean isMyBooking;

    // ===== 活动槽位字段（slotType=2时使用）=====

    /**
     * 活动名称（如"羽毛球畅打"）
     */
    private String activityName;

    /**
     * 活动图片
     */
    private List<String> imageUrls;

    /**
     * 参与者最低NTRP水平要求（范围1.0-7.0）
     */
    private Double minNtrpLevel;

    /**
     * 活动当前参与人数
     */
    private Integer currentParticipants;

    /**
     * 活动最多参与人数
     */
    private Integer maxParticipants;

    /**
     * 活动单人价格
     */
    private BigDecimal unitPrice;


    /**
     * 锁场批次
     */
    private Long merchantBatchId;

    /**
     * 商家锁场操作人昵称
     */
    private String displayName;

    /**
     * 使用人名称
     */
    private String userName;

    /**
     * 使用者电话
     */
    private String userPhone;

    /**
     * 构建普通槽位VO
     */
    public static VenueBookingSlotVo buildNormalSlot(
            VenueBookingSlotTemplate template,
            VenueBookingSlotRecord record,
            Map<LocalTime, BigDecimal> priceMap,
            Long userId,
            Map<Long, String> staffNameMap) {

        Long templateId = template.getBookingSlotTemplateId();

        // 如果没有记录，则默认为可预订状态
        int status = record != null ? record.getStatus() : BookingSlotStatus.AVAILABLE.getValue();

        // slotId: 如果有记录使用记录ID，否则使用模板ID
        Long slotId = record != null ? record.getBookingSlotRecordId() : templateId;

        String displayName = null;
        if (record != null && record.getOperatorId() != null && staffNameMap != null) {
            displayName = staffNameMap.get(record.getOperatorId());
        }

        String statusDesc;
        boolean isAvailable;
        try {
            BookingSlotStatus statusEnum = BookingSlotStatus.fromValue(status);
            statusDesc = statusEnum.getDescription();
            isAvailable = statusEnum == BookingSlotStatus.AVAILABLE;
        } catch (Exception e) {
            statusDesc = "未知状态";
            isAvailable = false;
        }

        // 从预先批量获取的priceMap中获取价格（O(1)查询，无数据库访问）
        BigDecimal price = priceMap.get(template.getStartTime());
        if (price == null) {
            price = new BigDecimal(999);
        }

        // 判断是否是本人预定的槽位
        Boolean isMyBooking = record != null && record.getOperatorId() != null && record.getOperatorId().equals(userId);

        return VenueBookingSlotVo.builder()
                .bookingSlotId(slotId)
                .templateId(templateId)
                .slotType(SlotTypeEnum.NORMAL.getCode())
                .startTime(template.getStartTime())
                .endTime(template.getEndTime())
                .status(status)
                .statusDesc(statusDesc)
                .isAvailable(isAvailable)
                .price(price)
                .isMyBooking(isMyBooking)
                .lockedType(record != null ? record.getLockedType() : null)
                .lockReason(record != null ? record.getLockReason() : null)
                .merchantBatchId(record != null ? record.getMerchantBatchId() : null)
                .userName(record != null ? record.getUserName() : null)
                .userPhone(record != null ? record.getUserPhone() : null)
                .orderId(record != null && record.getOrderId() != null ? String.valueOf(record.getOrderId()) : null)
                .displayName(displayName) //商家锁场操作人昵称
                .build();
    }

    /**
     * 构建活动槽位VO（修复版 - 完整映射所有字段）
     */
    public static VenueBookingSlotVo buildActivitySlot(VenueActivity activity, Set<Long> userRegisteredActivityIds) {
        // 判断用户是否报名了该活动
        Boolean isMyBooking = userRegisteredActivityIds != null &&
                userRegisteredActivityIds.contains(activity.getActivityId());

        // 判断活动状态
        int status;
        String statusDesc;
        boolean isAvailable;

        if (activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            status = BookingSlotStatus.LOCKED_IN.getValue();
            statusDesc = "活动已满员";
            isAvailable = false;
        } else {
            status = BookingSlotStatus.AVAILABLE.getValue();
            statusDesc = "可报名";
            isAvailable = true;
        }

        return VenueBookingSlotVo.builder()
                .bookingSlotId(activity.getActivityId())
                .slotType(SlotTypeEnum.ACTIVITY.getCode())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .merchantBatchId(activity.getMerchantBatchId())
                // 活动特有字段
                .activityName(activity.getActivityName())
                .imageUrls(activity.getImageUrls())
                .minNtrpLevel(activity.getMinNtrpLevel())
                .currentParticipants(activity.getCurrentParticipants())
                .maxParticipants(activity.getMaxParticipants())
                .unitPrice(activity.getUnitPrice())
                // 状态字段
                .status(status)
                .statusDesc(statusDesc)
                .isAvailable(isAvailable)
                .price(activity.getUnitPrice())
                .isMyBooking(isMyBooking)
                .build();
    }
}
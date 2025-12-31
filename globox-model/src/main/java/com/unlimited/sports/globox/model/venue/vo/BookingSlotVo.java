package com.unlimited.sports.globox.model.venue.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 预订槽位VO
 * 支持两种槽位类型：
 * 1. 普通槽位（slotType=1）：用户可以直接预订，包含价格和状态信息
 * 2. 活动槽位（slotType=2）：被活动占用，包含活动信息
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
public class BookingSlotVo {

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
     * 参与者最低NTRP水平要求（范围1.0-7.0）
     */
    private BigDecimal minNtrpLevel;

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
}

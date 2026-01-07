package com.unlimited.sports.globox.model.venue.vo;

import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.enums.CourtType;
import com.unlimited.sports.globox.model.venue.enums.GroundType;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 场地槽位VO
 */
@Data
@Builder
@Slf4j
public class CourtSlotVo {

    @NonNull
    private Long courtId;

    @NonNull
    private String courtName;

    @NonNull
    private Integer courtType;

    @NonNull
    private String courtTypeDesc;

      
    private Integer groundType;

      
    private String groundTypeDesc;

    @NonNull
    private List<BookingSlotVo> slots;

    /**
     * 构建场地槽位VO
     */
    public static CourtSlotVo buildVo(
            Court court,
            List<VenueBookingSlotTemplate> templates,
            Map<Long, VenueBookingSlotRecord> recordMap,
            Map<LocalTime, BigDecimal> priceMap,
            Map<Long, VenueActivity> activityMap,
            Map<Long, Long> activityLockedSlots,
            Long userId) {

        // 根据模板生成槽位VO，同一活动只在第一个槽位处显示（使用活动的完整时间段）
        Set<Long> processedActivityIds = new HashSet<>();

        List<BookingSlotVo> slots = templates.stream()
                .flatMap(template -> {
                    Long activityId = activityLockedSlots.get(template.getBookingSlotTemplateId());
                    // 如果被活动占用
                    if (activityId != null) {
                        // 使用 Set.add() 返回值判断是否第一次遇到此活动
                        if (processedActivityIds.add(activityId)) {
                            VenueActivity activity = activityMap.get(activityId);
                            if (activity != null) {
                                return Stream.of(BookingSlotVo.buildActivitySlot(activity));
                            }
                        }
                        // 不添加到结果
                        return Stream.empty();
                    }
                    // 普通槽位直接构建返回
                    BookingSlotVo slot = BookingSlotVo.buildNormalSlot(
                            template,
                            recordMap.get(template.getBookingSlotTemplateId()),
                            priceMap,
                            userId
                    );
                    return Stream.of(slot);
                })
                .collect(Collectors.toList());

        // 构建场地VO
        String courtTypeDesc;
        String groundTypeDesc;
        courtTypeDesc = CourtType.fromValue(court.getCourtType()).getDescription();
        groundTypeDesc = GroundType.fromValue(court.getGroundType()).getDescription();
        return CourtSlotVo.builder()
                .courtId(court.getCourtId())
                .courtName(court.getName())
                .courtType(court.getCourtType())
                .courtTypeDesc(courtTypeDesc)
                .groundType(court.getGroundType())
                .groundTypeDesc(groundTypeDesc)
                .slots(slots)
                .build();
    }
}

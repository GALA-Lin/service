package com.unlimited.sports.globox.model.merchant.vo;

import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.merchant.entity.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 商家端场地槽位视图统一构建类
 * @since 2025-12-27
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantCourtSlotVo {

    /**
     * 场地ID
     */
    private Long courtId;

    /**
     * 场地名称
     */
    private String courtName;

    /**
     * 场地类型
     */
    private String courtType;

    /**
     * 槽位列表
     */
    private List<VenueBookingSlotVo> slots;

    /**
     * 可用槽位数量
     */
    private Integer availableCount;

    /**
     * 总槽位数量
     */
    private Integer totalCount;

    /**
     * 统一构建场地槽位VO
     *
     * @param court                     场地信息
     * @param templates                 槽位模板列表
     * @param recordMap                 槽位记录映射 (templateId -> record)
     * @param priceMap                  价格映射 (startTime -> price)
     * @param activityMap               活动映射 (activityId -> activity)
     * @param activityLockedSlots       活动占用的槽位映射 (templateId -> activityId)
     * @param userId                    用户ID（商家端可传null）
     * @param userRegisteredActivityIds 用户已报名的活动ID集合（商家端可传null）
     * @param staffNameMap              职工信息
     * @return MerchantCourtSlotVo
     */
    public static MerchantCourtSlotVo buildVo(
            Court court,
            List<VenueBookingSlotTemplate> templates,
            Map<Long, VenueBookingSlotRecord> recordMap,
            Map<LocalTime, BigDecimal> priceMap,
            Map<Long, VenueActivity> activityMap,
            Map<Long, Long> activityLockedSlots,
            Long userId,
            Set<Long> userRegisteredActivityIds, Map<Long, String> staffNameMap) {

        log.debug("构建场地槽位VO - courtId: {}, 模板数: {}, 活动数: {}",
                court.getCourtId(), templates.size(), activityMap.size());

        // 使用 Set 记录已处理的活动ID，防止重复显示
        Set<Long> processedActivityIds = new HashSet<>();

        // 构建槽位列表 - 每个槽位都正常显示
        List<VenueBookingSlotVo> slots = templates.stream()
                .map(template -> {
                    Long templateId = template.getBookingSlotTemplateId();

                    // 检查该槽位是否被活动占用
                    Long activityId = activityLockedSlots.get(templateId);

                    if (activityId != null) {
                        // 活动槽位
                        VenueActivity activity = activityMap.get(activityId);
                        if (activity != null) {
                            log.debug("活动槽位 - templateId: {}, activityId: {}, activityName: {}",
                                    templateId, activityId, activity.getActivityName());
                            return VenueBookingSlotVo.buildActivitySlot(
                                    activity, userRegisteredActivityIds);
                        } else {
                            log.warn("槽位 {} 关联的活动不存在 - activityId: {}", templateId, activityId);
                            // 活动不存在时，显示为不可用槽位
                            VenueBookingSlotRecord record = recordMap.get(templateId);
                            return VenueBookingSlotVo.buildNormalSlot(
                                    template, record, priceMap, userId, staffNameMap);
                        }
                    }
                        // 普通槽位
                    VenueBookingSlotRecord record = recordMap.get(templateId);
                    return VenueBookingSlotVo.buildNormalSlot(
                            template, record, priceMap, userId, staffNameMap);
                    
                })
                .collect(Collectors.toList());

        // 统计可用槽位数量
        int availableCount = (int) slots.stream()
                .filter(slot -> slot.getIsAvailable() != null && slot.getIsAvailable())
                .count();

        log.debug("场地 {} 槽位构建完成 - 总数: {}, 可用: {}, 活动数: {}",
                court.getCourtId(), slots.size(), availableCount, processedActivityIds.size());

        return MerchantCourtSlotVo.builder()
                .courtId(court.getCourtId())
                .courtName(court.getName())
                .courtType(getCourtTypeName(court.getCourtType()))
                .slots(slots)
                .availableCount(availableCount)
                .totalCount(slots.size())
                .build();
    }

    /**
     * 获取场地类型名称
     */
    private static String getCourtTypeName(Integer courtType) {
        if (courtType == null) {
            return "未知";
        }
        return switch (courtType) {
            case 1 -> "室内";
            case 2 -> "室外";
            case 3 -> "风雨场";
            case 4 -> "半封闭";
            default -> "其他";
        };
    }
}
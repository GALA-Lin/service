package com.unlimited.sports.globox.venue.admin.util;

import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 槽位模板生成器
 *
 * 根据营业时间生成槽位模板，每30分钟一个槽位
 * 最后一个槽位的endTime统一为23:59:59
 */
@Slf4j
public class SlotTemplateGenerator {

    private static final long SLOT_DURATION_MINUTES = 30L;

    /**
     * 根据营业时间为场地生成槽位模板
     *
     * @param courtId 场地ID
     * @param openTime 营业开始时间
     * @param closeTime 营业结束时间
     * @return 槽位模板列表
     */
    public static List<VenueBookingSlotTemplate> generateDailySlots(Long courtId, LocalTime openTime, LocalTime closeTime) {
        List<VenueBookingSlotTemplate> slots = new ArrayList<>();

        // 处理closeTime为00:00:00的情况，统一转换为23:59:59
        LocalTime effectiveCloseTime = LocalTime.MIDNIGHT.equals(closeTime) ? LocalTime.of(23, 59, 59) : closeTime;

        LocalTime currentStart = openTime;
        while (currentStart.isBefore(effectiveCloseTime) && !currentStart.equals(effectiveCloseTime)) {
            LocalTime slotEndTime = currentStart.plusMinutes(SLOT_DURATION_MINUTES);
            
            // 如果下一个槽位结束时间超过营业结束时间，则以营业结束时间为准
            // 同时处理跨午夜的情况（slotEndTime变成00:00:00）
            if (slotEndTime.equals(LocalTime.MIDNIGHT) || slotEndTime.isAfter(effectiveCloseTime)) {
                slotEndTime = effectiveCloseTime;
            }

            VenueBookingSlotTemplate slot = VenueBookingSlotTemplate.builder()
                    .courtId(courtId)
                    .startTime(currentStart)
                    .endTime(slotEndTime)
                    .build();

            slots.add(slot);
            
            // 如果已经到达营业结束时间，退出循环
            if (slotEndTime.equals(effectiveCloseTime)) {
                break;
            }
            
            currentStart = slotEndTime;
        }

        log.info("为场地 {} 生成了 {} 个槽位模板（营业时间：{} - {}）", courtId, slots.size(), openTime, effectiveCloseTime);
        return slots;
    }

    /**
     * 批量为多个场地生成槽位模板（根据营业时间）
     *
     * @param courtIds 场地ID列表
     * @param openTime 营业开始时间
     * @param closeTime 营业结束时间
     * @return 所有槽位模板列表
     */
    public static List<VenueBookingSlotTemplate> generateSlotsForMultipleCourts(List<Long> courtIds, LocalTime openTime, LocalTime closeTime) {
        List<VenueBookingSlotTemplate> allSlots = new ArrayList<>();

        for (Long courtId : courtIds) {
            allSlots.addAll(generateDailySlots(courtId, openTime, closeTime));
        }

        log.info("总共生成了 {} 个槽位模板，涉及 {} 个场地", allSlots.size(), courtIds.size());
        return allSlots;
    }
}

package com.unlimited.sports.globox.venue.admin.util;

import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 槽位模板生成器
 *
 * 生成24小时的槽位模板，每30分钟一个槽位
 * 时间范围：00:00 - 23:30 (48个槽位)
 */
@Slf4j
public class SlotTemplateGenerator {

    private static final long SLOT_DURATION_MINUTES = 30L;
    private static final int TOTAL_SLOTS_PER_DAY = 48;  // 24小时 / 0.5小时

    /**
     * 为场地生成24小时的槽位模板
     *
     * @param courtId 场地ID
     * @return 48个槽位模板列表
     */
    public static List<VenueBookingSlotTemplate> generateDailySlots(Long courtId) {
        List<VenueBookingSlotTemplate> slots = new ArrayList<>(TOTAL_SLOTS_PER_DAY);

        LocalTime startTime = LocalTime.of(0, 0, 0);  // 00:00:00

        for (int i = 0; i < TOTAL_SLOTS_PER_DAY; i++) {
            LocalTime slotStartTime = startTime.plusMinutes(i * SLOT_DURATION_MINUTES);
            LocalTime slotEndTime = slotStartTime.plusMinutes(SLOT_DURATION_MINUTES);

            // 处理最后一个槽位：23:30 -> 00:00 (下一天)
            if (i == TOTAL_SLOTS_PER_DAY - 1) {
                slotEndTime = LocalTime.of(0, 0, 0);
            }

            VenueBookingSlotTemplate slot = VenueBookingSlotTemplate.builder()
                    .courtId(courtId)
                    .startTime(slotStartTime)
                    .endTime(slotEndTime)
                    .build();

            slots.add(slot);
        }

        log.info("为场地 {} 生成了 {} 个槽位模板", courtId, slots.size());
        return slots;
    }

    /**
     * 批量为多个场地生成槽位模板
     *
     * @param courtIds 场地ID列表
     * @return 所有槽位模板列表
     */
    public static List<VenueBookingSlotTemplate> generateSlotsForMultipleCourts(List<Long> courtIds) {
        List<VenueBookingSlotTemplate> allSlots = new ArrayList<>();

        for (Long courtId : courtIds) {
            allSlots.addAll(generateDailySlots(courtId));
        }

        log.info("总共生成了 {} 个槽位模板，涉及 {} 个场地", allSlots.size(), courtIds.size());
        return allSlots;
    }
}

package com.unlimited.sports.globox.venue.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 时间区间拆分工具类
 * 用于优雅地处理时间区间的拆分，支持跨午夜的情况
 */
@Slf4j
public class TimeSlotSplitUtil {

    private static final int DEFAULT_INTERVAL_MINUTES = 30;

    /**
     * 简单的时间槽位拆分（同一天内）
     *
     * @param startTime 开始时间
     * @param closeTime 结束时间
     * @param action 对每个槽位的操作
     */
    public static void splitTimeSlots(LocalTime startTime, LocalTime closeTime, Consumer<TimeSlot> action) {
        splitTimeSlots(startTime, closeTime, DEFAULT_INTERVAL_MINUTES, action);
    }

    /**
     * 自定义间隔的时间槽位拆分（同一天内）
     *
     * @param startTime 开始时间
     * @param closeTime 结束时间
     * @param intervalMinutes 间隔分钟数
     * @param action 对每个槽位的操作
     */
    public static void splitTimeSlots(LocalTime startTime, LocalTime closeTime, int intervalMinutes, Consumer<TimeSlot> action) {
        // 使用LocalDateTime处理，基准日期为2000-01-01
        LocalDate baseDate = LocalDate.of(2000, 1, 1);
        LocalDateTime startDateTime = LocalDateTime.of(baseDate, startTime);
        LocalDateTime endDateTime;

        // 如果closeTime小于startTime，说明跨越了午夜，endTime应该属于next day
        if (closeTime.isBefore(startTime)) {
            endDateTime = LocalDateTime.of(baseDate.plusDays(1), closeTime);
        } else {
            endDateTime = LocalDateTime.of(baseDate, closeTime);
        }

        LocalDateTime current = startDateTime;
        while (current.isBefore(endDateTime)) {
            LocalDateTime nextTime = current.plusMinutes(intervalMinutes);
            LocalDateTime slotEnd = nextTime.isAfter(endDateTime) ? endDateTime : nextTime;

            TimeSlot slot = TimeSlot.builder()
                    .startTime(current.toLocalTime())
                    .endTime(slotEnd.toLocalTime())
                    .build();

            action.accept(slot);

            current = slotEnd;
        }
    }

    /**
     * 返回拆分后的槽位列表（同一天内）
     *
     * @param startTime 开始时间
     * @param closeTime 结束时间
     * @return 槽位列表
     */
    public static List<TimeSlot> splitTimeSlots(LocalTime startTime, LocalTime closeTime) {
        return splitTimeSlots(startTime, closeTime, DEFAULT_INTERVAL_MINUTES);
    }

    /**
     * 返回拆分后的槽位列表（自定义间隔）
     *
     * @param startTime 开始时间
     * @param closeTime 结束时间
     * @param intervalMinutes 间隔分钟数
     * @return 槽位列表
     */
    public static List<TimeSlot> splitTimeSlots(LocalTime startTime, LocalTime closeTime, int intervalMinutes) {
        List<TimeSlot> slots = new ArrayList<>();
        splitTimeSlots(startTime, closeTime, intervalMinutes, slots::add);
        return slots;
    }

    /**
     * 时间槽位对象
     */
    public static class TimeSlot {
        private LocalTime startTime;
        private LocalTime endTime;

        private TimeSlot(LocalTime startTime, LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public static TimeSlotBuilder builder() {
            return new TimeSlotBuilder();
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        @Override
        public String toString() {
            return startTime + "-" + endTime;
        }

        public static class TimeSlotBuilder {
            private LocalTime startTime;
            private LocalTime endTime;

            public TimeSlotBuilder startTime(LocalTime startTime) {
                this.startTime = startTime;
                return this;
            }

            public TimeSlotBuilder endTime(LocalTime endTime) {
                this.endTime = endTime;
                return this;
            }

            public TimeSlot build() {
                return new TimeSlot(startTime, endTime);
            }
        }
    }
}

package com.unlimited.sports.globox.model.coach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 课程状态枚举（不落库，动态计算）
 *
 * @since 2026/02/02
 */
@Getter
@AllArgsConstructor
public enum CourseStatusEnum {

    /**
     * 1 - 待上课：课程时间未到
     */
    UPCOMING(1, "待上课"),

    /**
     * 2 - 上课中：课程正在进行
     */
    IN_PROGRESS(2, "上课中"),

    /**
     * 3 - 已完成：课程已结束
     */
    COMPLETED(3, "已完成"),

    /**
     * 4 - 已取消：课程已取消（仅自定义日程）
     */
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String description;

    /**
     * 根据时间动态计算平台预约课程状态
     *
     * @param scheduleDate 课程日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 课程状态
     */
    public static CourseStatusEnum calculatePlatformStatus(
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduleStart = LocalDateTime.of(scheduleDate, startTime);
        LocalDateTime scheduleEnd = LocalDateTime.of(scheduleDate, endTime);

        if (now.isBefore(scheduleStart)) {
            // 课程未开始
            return UPCOMING;
        } else if (now.isAfter(scheduleEnd)) {
            // 课程已结束
            return COMPLETED;
        } else {
            // 课程进行中
            return IN_PROGRESS;
        }
    }

    /**
     * 根据时间和状态动态计算自定义日程课程状态
     *
     * @param status 自定义日程状态（1=正常，2=已取消，3=已完成）
     * @param scheduleDate 课程日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 课程状态
     */
    public static CourseStatusEnum calculateCustomStatus(
            Integer status,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime) {

        // 1. 先检查数据库状态
        if (status != null) {
            if (status == 2) {
                // 已取消
                return CANCELLED;
            } else if (status == 3) {
                // 已完成
                return COMPLETED;
            }
        }

        // 2. 状态为1（正常）或null，根据时间判断
        return calculatePlatformStatus(scheduleDate, startTime, endTime);
    }

    /**
     * 根据状态码获取描述
     */
    public static String getDescription(Integer code) {
        if (code == null) {
            return "未知状态";
        }
        for (CourseStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status.description;
            }
        }
        return "未知状态";
    }
}
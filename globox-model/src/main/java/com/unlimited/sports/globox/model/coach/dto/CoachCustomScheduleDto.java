package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 自定义日程DTO
 */
@Data
public class CoachCustomScheduleDto {

    /**
     * 教练ID
     */
    @NotNull(message = "教练ID不能为空")
    private Long coachUserId;

    /**
     * 学员姓名
     */
    @Size(max = 20, message = "学员姓名不能超过20个字符")
    private String studentName;

    /**
     * 学员手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String studentPhone;


    /**
     * 日期
     */
    @NotNull(message = "日期不能为空")
    private LocalDate scheduleDate;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    /**
     * 课程名称
     */
    @Size(max = 200, message = "地点名称不能超过200字")
    private String venueName;

    /**
     * 地点地址
     */
    @Size(max = 500, message = "详细地址不能超过500字")
    private String venueAddress;

    /**
     * 服务类型
     */
    private Integer coachServiceType;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500字")
    private String remark;

    /**
     * 提醒时间
     */
    private Integer reminderMinutes;


}

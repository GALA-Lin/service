// globox-dubbo-api/src/main/java/com/unlimited/sports/globox/dubbo/coach/dto/CoachSlotSnapshotDto.java
package com.unlimited.sports.globox.dubbo.coach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 教练时段快照DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachSlotSnapshotDto implements Serializable {

    /**
     * 时段记录ID
     */
    private Long slotRecordId;

    /**
     * 时段模板ID
     */
    @NotNull
    private Long slotTemplateId;

    /**
     * 预定日期
     */
    @NotNull
    private LocalDate bookingDate;

    /**
     * 开始时间
     */
    @NotNull
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 时段持续时间（分钟）
     */
    private Integer durationMinutes;

    /**
     * 时段价格
     */
    @NotNull
    private BigDecimal price;

    /**
     * 服务类别
     */
    private Integer serviceType;

    /**
     * 服务类别描述
     */
    private String serviceTypeDesc;

    /**
     * 教练接受的区域
     */
    private List<String> acceptableAreas;

    /**
     * 教练接受的年龄范围
     */
    private String venueRequirementDesc;
}
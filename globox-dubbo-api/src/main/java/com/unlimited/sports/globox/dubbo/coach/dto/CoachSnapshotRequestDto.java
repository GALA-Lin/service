package com.unlimited.sports.globox.dubbo.coach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachSnapshotRequestDto implements Serializable {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "教练ID不能为空")
    private Long coachUserId;

    @NotNull(message = "预约日期不能为空")
    private LocalDate bookingDate;

    /**
     * 时段模板ID列表
     */
    @NotNull(message = "时段模板ID列表不能为空")
    private List<Long> slotIds;

    /**
     * 服务类型ID
     */
    private Long serviceTypeId;
}
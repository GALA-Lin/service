package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 批量创建/更新教练课程类型DTO
 * @since 2026/1/8 10:07
 */
@Data
public class CoachCourseTypeBatchDto {

    /**
     * 教练ID
     */
    private Long coachUserId;

    /**
     * 课程类型列表
     * 最多支持4种服务类型(一对一教学、一对一陪练、一对二、小班)
     */
    @NotEmpty(message = "课程类型列表不能为空")
    @Size(min = 1, max = 4, message = "最多支持创建4种课程类型")
    @Valid
    private List<CoachCourseTypeDto> courseTypes;
}
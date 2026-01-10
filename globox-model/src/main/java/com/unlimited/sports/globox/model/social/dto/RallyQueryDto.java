package com.unlimited.sports.globox.model.social.dto;

import com.unlimited.sports.globox.common.result.PaginationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 约球查询参数
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RallyQueryDto  {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Integer page;

    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize;

    /**
     * 区域
     */
    private String area;
    /**
     * 时间范围
     */
    private Integer timeRange;
    /**
     * 性别限制
     */
    private Integer genderLimit;
    /**
     * NTRP最小值
     */
    private Double ntrpMin;
    /**
     * NTRP最大值
     */
    private Double ntrpMax;
    /**
     * 活动类型
     */
    private Integer activityType;
    /**
     * 时间范围开始时间
     */
    private String timeRangeStart;
    /**
     * 时间范围结束时间
     */
    private String timeRangeEnd;


}

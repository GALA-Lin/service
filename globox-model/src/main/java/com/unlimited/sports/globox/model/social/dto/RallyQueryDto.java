package com.unlimited.sports.globox.model.social.dto;

import com.unlimited.sports.globox.common.result.PaginationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * 约球查询参数
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RallyQueryDto  {
    /**
     * 区域
     */
    private List<String> area;
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

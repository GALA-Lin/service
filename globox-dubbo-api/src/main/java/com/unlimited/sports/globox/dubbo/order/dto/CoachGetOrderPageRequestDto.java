package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 教练模块获取订单分页 - 请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachGetOrderPageRequestDto implements Serializable {

    /**
     * 教练 ID
     */
    @NotNull(message = "教练ID不能为空")
    private Long coachId;

    /**
     * 页码（从 1 开始）
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于等于 1")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于等于 1")
    private Integer pageSize = 10;
}

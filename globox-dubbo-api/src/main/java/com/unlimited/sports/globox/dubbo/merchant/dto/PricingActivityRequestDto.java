package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 活动报名价格查询请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingActivityRequestDto implements Serializable {

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotNull(message = "活动 ID 不能为空")
    private Long activityId;

//    @NotNull(message = "手机号不能为空")
    private String userPhone;

    /**
     * 报名名额数（支持帮他人报名）
     */
    @NotNull(message = "报名名额数不能为空")
    @Min(value = 1, message = "报名名额数最少为1")
    private Integer quantity;

}

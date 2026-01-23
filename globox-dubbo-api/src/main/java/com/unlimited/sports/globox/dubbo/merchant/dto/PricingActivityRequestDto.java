package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * d
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

}

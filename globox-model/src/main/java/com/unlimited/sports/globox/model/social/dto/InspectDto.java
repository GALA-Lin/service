package com.unlimited.sports.globox.model.social.dto;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InspectDto {

    /**
     * 帖子ID，审核的目标帖子
     */
    @NotNull
    private Long postId;

    /**
     * 申请人ID，发起审核的用户
     */
    @NotNull
    private Long applicantId;

    /**
     * 审核结果: 0-待审核，1-通过，2-拒绝
     */
    @NotNull
    private Integer inspectResult;
}

package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 账号注销确认请求
 */
@Data
@Schema(description = "账号注销确认请求")
public class CancelAccountConfirmRequest {

    @NotBlank(message = "注销确认凭证不能为空")
    @Schema(description = "注销确认凭证", example = "b7f2b9c9-9f41-4e30-9c8a-7d3e1f7c6a88")
    private String cancelToken;
}

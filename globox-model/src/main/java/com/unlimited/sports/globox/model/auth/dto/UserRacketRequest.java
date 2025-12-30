package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户球拍请求项
 */
@Data
@Schema(description = "用户球拍请求项")
public class UserRacketRequest {

    @Schema(description = "球拍型号ID", example = "1001")
    private Long racketModelId;

    @Schema(description = "是否主力拍", example = "true")
    private Boolean isPrimary;
}

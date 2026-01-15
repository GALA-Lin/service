package com.unlimited.sports.globox.model.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户球拍视图
 */
@Data
@Schema(description = "用户球拍视图")
public class UserRacketVo {

    @Schema(description = "球拍型号ID", example = "1001")
    private Long racketModelId;

    @Schema(description = "球拍型号名称", example = "Pure Drive 98")
    private String racketModelName;

    @Schema(description = "球拍全名（品牌+系列+型号）", example = "Babolat Pure Drive 98")
    private String racketModelFullName;

    @Schema(description = "是否主力拍", example = "true")
    private Boolean isPrimary;
}

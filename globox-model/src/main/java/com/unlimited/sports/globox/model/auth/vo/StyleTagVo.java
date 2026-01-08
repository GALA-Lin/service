package com.unlimited.sports.globox.model.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 球风标签视图
 */
@Data
@Schema(description = "球风标签视图")
public class StyleTagVo {

    @Schema(description = "标签ID", example = "1")
    private Long tagId;

    @Schema(description = "标签名称", example = "强力发球")
    private String name;
}

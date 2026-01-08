package com.unlimited.sports.globox.model.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 资料可选项视图
 */
@Data
@Schema(description = "资料可选项视图")
public class ProfileOptionsVo {

    @Schema(description = "球拍字典选项")
    private List<RacketDictNodeVo> racketOptions;

    @Schema(description = "球风标签选项")
    private List<StyleTagVo> styleTags;
}

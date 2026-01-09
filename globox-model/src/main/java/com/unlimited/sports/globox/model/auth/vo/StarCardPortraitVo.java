package com.unlimited.sports.globox.model.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 球星卡肖像视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "球星卡肖像视图")
public class StarCardPortraitVo {

    @Schema(description = "球星卡肖像URL",
            example = "https://example.com/portrait.jpg")
    private String portraitUrl;
}


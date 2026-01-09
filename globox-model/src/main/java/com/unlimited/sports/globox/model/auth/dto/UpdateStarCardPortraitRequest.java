package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新球星卡肖像请求
 */
@Data
@Schema(description = "更新球星卡肖像请求")
public class UpdateStarCardPortraitRequest {

    @Schema(description = "球星卡肖像URL（传 null 或空字符串表示删除）",
            example = "https://example.com/portrait.jpg")
    private String portraitUrl;
}


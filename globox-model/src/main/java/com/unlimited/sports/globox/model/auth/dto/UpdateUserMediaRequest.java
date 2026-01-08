package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户媒体更新请求
 */
@Data
@Schema(description = "用户媒体更新请求（全量替换）")
public class UpdateUserMediaRequest {

    @Valid
    @Schema(description = "媒体列表（完整列表，会完全替换现有媒体）", required = true)
    private List<UserMediaRequest> mediaList;
}


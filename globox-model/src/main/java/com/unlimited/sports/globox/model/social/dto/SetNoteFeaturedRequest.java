package com.unlimited.sports.globox.model.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量设置笔记精选状态请求
 */
@Data
@Schema(description = "批量设置笔记精选状态请求")
public class SetNoteFeaturedRequest {

    @NotEmpty
    @Schema(description = "笔记ID列表", example = "[1,2,3]")
    private List<Long> noteIds;

    @NotNull
    @Schema(description = "是否设置为精选", example = "true")
    private Boolean featured;
}

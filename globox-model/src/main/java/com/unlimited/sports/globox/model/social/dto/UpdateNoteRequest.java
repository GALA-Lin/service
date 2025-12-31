package com.unlimited.sports.globox.model.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * 更新笔记请求
 */
@Data
@Schema(description = "更新笔记请求")
public class UpdateNoteRequest {

    @Schema(description = "标题", example = "今天打球收获分享")
    private String title;

    @NotBlank(message = "正文不能为空")
    @Schema(description = "正文", example = "今天练习了发球和截击，感觉有提升。", required = true)
    private String content;

    @Schema(description = "是否允许评论", example = "true")
    private Boolean allowComment;

    @Pattern(regexp = "^(IMAGE|VIDEO)$", message = "媒体类型必须是 IMAGE 或 VIDEO")
    @Schema(description = "媒体类型：IMAGE-图片，VIDEO-视频", example = "IMAGE", allowableValues = {"IMAGE", "VIDEO"})
    private String mediaType;

    @Valid
    @Schema(description = "媒体列表（全量替换）")
    private List<NoteMediaRequest> mediaList;
}

package com.unlimited.sports.globox.model.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

/**
 * 直接发布笔记请求
 */
@Data
@Schema(description = "直接发布笔记请求")
public class DirectPublishNoteRequest {

    @Schema(description = "标题", example = "今天打球收获分享")
    @Size(max = 20, message = "笔记标题最多20个字符")
    private String title;

    @NotBlank(message = "正文不能为空")
    @Schema(description = "正文", example = "今天练习了发球和截击，感觉有提升。", required = true)
    private String content;

    @Schema(description = "是否允许评论", example = "true", defaultValue = "true")
    private Boolean allowComment;

    @NotBlank(message = "媒体类型不能为空")
    @Pattern(regexp = "^(IMAGE|VIDEO)$", message = "媒体类型必须是 IMAGE 或 VIDEO")
    @Schema(description = "媒体类型：IMAGE-图片，VIDEO-视频", example = "IMAGE", required = true, allowableValues = {"IMAGE", "VIDEO"})
    private String mediaType;

    @NotNull(message = "媒体列表不能为空")
    @NotEmpty(message = "媒体列表不能为空")
    @Valid
    @Schema(description = "媒体列表", required = true)
    private List<NoteMediaRequest> mediaList;

    @Schema(description = "笔记标签列表（如果为空，默认添加 TENNIS_COMMUNITY）")
    private List<String> tags;
}

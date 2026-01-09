package com.unlimited.sports.globox.model.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * 保存草稿请求
 */
@Data
@Schema(description = "保存草稿请求")
public class SaveDraftRequest {

    @Schema(description = "笔记ID（更新草稿时必填，新建草稿时不传）", example = "123")
    private Long noteId;

    @Schema(description = "标题", example = "今天打球收获分享")
    private String title;

    @Schema(description = "正文", example = "今天练习了发球和截击，感觉有提升。")
    private String content;

    @Schema(description = "是否允许评论", example = "true", defaultValue = "true")
    private Boolean allowComment;

    @Pattern(regexp = "^(IMAGE|VIDEO)$", message = "媒体类型必须是 IMAGE 或 VIDEO")
    @Schema(description = "媒体类型：IMAGE-图片，VIDEO-视频", example = "IMAGE", allowableValues = {"IMAGE", "VIDEO"})
    private String mediaType;

    @Valid
    @Schema(description = "媒体列表")
    private List<NoteMediaRequest> mediaList;
}


package com.unlimited.sports.globox.model.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建评论请求
 */
@Data
@Schema(description = "创建评论请求")
public class CreateCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 300, message = "评论内容不能超过300个字符")
    @Schema(description = "评论内容", example = "说得很有道理！", required = true)
    private String content;

    @Schema(description = "父评论ID（回复评论时必填，一级评论为空）", example = "123")
    private Long parentCommentId;

    @Schema(description = "回复对象用户ID（回复评论时必填）", example = "456")
    private Long replyToUserId;
}




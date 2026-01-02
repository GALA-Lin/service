package com.unlimited.sports.globox.model.social.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 媒体上传结果视图
 */
@Data
@Schema(description = "媒体上传结果")
public class MediaUploadVo {

    @Schema(description = "文件访问URL", example = "https://cdn.example.com/note/image.jpg")
    private String url;

    @Schema(description = "原始文件名", example = "image.jpg")
    private String fileName;

    @Schema(description = "文件大小（字节）", example = "1024000")
    private Long size;
}


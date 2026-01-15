package com.unlimited.sports.globox.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 视频上传响应VO（包含自动生成的封面URL）
 * 仅用于用户媒体视频上传接口
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频上传结果（包含封面URL）")
public class VideoUploadVo {

    /**
     * 视频文件URL
     */
    @NonNull
    @Schema(description = "视频文件访问URL", example = "https://xxx.cos.ap-beijing.myqcloud.com/video.mp4")
    private String url;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名", example = "video.mp4")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小（字节）", example = "5245678")
    private Long fileSize;

    /**
     * 封面URL（自动生成，使用腾讯云COS截图功能）
     */
    @Schema(description = "封面URL（自动生成）", example = "https://xxx.cos.ap-beijing.myqcloud.com/video.mp4?ci-process=snapshot&time=0&format=jpg")
    private String coverUrl;
}


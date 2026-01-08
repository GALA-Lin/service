package com.unlimited.sports.globox.cos.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量上传文件结果VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "批量上传文件结果")
public class BatchUploadResultVo {

    @Schema(description = "上传成功的文件URL列表")
    private List<String> successUrls;

    @Schema(description = "上传失败的文件信息列表")
    private List<FailedFileInfo> failedFiles;

    @Schema(description = "上传成功的数量")
    private Integer successCount;

    @Schema(description = "上传失败的数量")
    private Integer failureCount;

    @Schema(description = "总上传文件数")
    private Integer totalCount;

    /**
     * 失败文件信息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "上传失败的文件信息")
    public static class FailedFileInfo {

        @Schema(description = "文件名称")
        private String fileName;

        @Schema(description = "失败原因")
        private String reason;
    }
}

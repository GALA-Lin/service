package com.unlimited.sports.globox.social.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.social.vo.MediaUploadVo;
import com.unlimited.sports.globox.social.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体上传控制器
 */
@RestController
@RequestMapping("/social/media/upload")
@Tag(name = "媒体上传模块", description = "笔记图片/视频上传接口")
@SecurityRequirement(name = "bearerAuth")
public class MediaUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/image")
    @Operation(summary = "上传图片", description = "上传笔记图片并返回可访问URL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上传成功"),
            @ApiResponse(responseCode = "3013", description = "文件上传失败"),
            @ApiResponse(responseCode = "3014", description = "文件大小超过限制"),
            @ApiResponse(responseCode = "3015", description = "文件类型不支持"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<MediaUploadVo> uploadImage(
            @Parameter(description = "图片文件", required = true)
            @RequestPart("file") MultipartFile file) {
        MediaUploadVo result = fileUploadService.uploadFile(file);
        return R.ok(result);
    }

    @PostMapping("/video")
    @Operation(summary = "上传视频", description = "上传笔记视频并返回可访问URL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上传成功"),
            @ApiResponse(responseCode = "3013", description = "文件上传失败"),
            @ApiResponse(responseCode = "3014", description = "文件大小超过限制"),
            @ApiResponse(responseCode = "3015", description = "文件类型不支持"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<MediaUploadVo> uploadVideo(
            @Parameter(description = "视频文件", required = true)
            @RequestPart("file") MultipartFile file) {
        MediaUploadVo result = fileUploadService.uploadFile(file);
        return R.ok(result);
    }
}


package com.unlimited.sports.globox.venue.controller;

import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.venue.service.IFileUploadService;
import com.unlimited.sports.globox.model.venue.vo.FileUploadVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/venue/file")
@Tag(name = "文件上传", description = "文件上传相关接口")
public class FileUploadController {

    @Autowired
    private IFileUploadService fileUploadService;



    @PostMapping("/review-images")
    public R<FileUploadVo> uploadReviewImage(
            @RequestParam("file") MultipartFile file) {
        log.info("开始上传评论图片，文件名: {}, 大小: {} bytes",
                file.getOriginalFilename(), file.getSize());

        String fileUrl = fileUploadService.uploadFile(file, FileTypeEnum.REVIEW_IMAGE);

        FileUploadVo vo = new FileUploadVo(
                fileUrl,
                file.getOriginalFilename(),
                file.getSize()
        );

        log.info("评论图片上传成功: {}", fileUrl);
        return R.ok(vo);
    }

}

package com.unlimited.sports.globox.coach.controller;

import com.unlimited.sports.globox.coach.service.ICoachFileUploadService;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.cos.vo.BatchUploadResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;

/**
 * @since 2026/1/12
 * 教练文件上传Controller
 */
@Slf4j
@RestController
@RequestMapping("/coach/file")
@Validated
public class CoachUploadFileController {

    @Autowired
    private ICoachFileUploadService coachFileUploadService;

    /**
     * 批量上传教学图片
     *
     * @param coachUserId 教练用户ID（从请求头获取）
     * @param files 图片文件数组
     * @return 上传结果
     */
    @PostMapping("/work-photos")
    public R<BatchUploadResultVo> batchUploadWorkPhotos(
            @RequestHeader(HEADER_USER_ID) Long coachUserId,
            @RequestParam("files") MultipartFile[] files) {

        log.info("开始批量上传教学图片 - 教练: {}, 文件数量: {}",
                coachUserId, files != null ? files.length : 0);

        // 批量上传文件
        BatchUploadResultVo result = coachFileUploadService.batchUploadFiles(files, FileTypeEnum.COACH_WORK_PHOTO);

        log.info("批量教学图片上传完成 - 教练: {}, 成功: {}, 失败: {}",
                coachUserId, result.getSuccessCount(), result.getFailureCount());

        return R.ok(result);
    }

    /**
     * 批量上传教学视频
     *
     * @param coachUserId 教练用户ID（从请求头获取）
     * @param files 视频文件数组
     * @return 上传结果
     */
    @PostMapping("/work-videos")
    public R<BatchUploadResultVo> batchUploadWorkVideos(
            @RequestHeader(HEADER_USER_ID) Long coachUserId,
            @RequestParam("files") MultipartFile[] files) {

        log.info("开始批量上传教学视频 - 教练: {}, 文件数量: {}",
                coachUserId, files != null ? files.length : 0);

        // 批量上传文件
        BatchUploadResultVo result = coachFileUploadService.batchUploadFiles(files, FileTypeEnum.COACH_WORK_VIDEO);

        log.info("批量教学视频上传完成 - 教练: {}, 成功: {}, 失败: {}",
                coachUserId, result.getSuccessCount(), result.getFailureCount());

        return R.ok(result);
    }

    /**
     * 批量上传证书附件
     *
     * @param coachUserId 教练用户ID（从请求头获取）
     * @param files 证书文件数组
     * @return 上传结果
     */
    @PostMapping("/certification-files")
    public R<BatchUploadResultVo> batchUploadCertificationFiles(
            @RequestHeader(HEADER_USER_ID) Long coachUserId,
            @RequestParam("files") MultipartFile[] files) {

        log.info("开始批量上传证书附件 - 教练: {}, 文件数量: {}",
                coachUserId, files != null ? files.length : 0);

        // 批量上传文件
        BatchUploadResultVo result = coachFileUploadService.batchUploadFiles(files, FileTypeEnum.COACH_CERTIFICATION);

        log.info("批量证书附件上传完成 - 教练: {}, 成功: {}, 失败: {}",
                coachUserId, result.getSuccessCount(), result.getFailureCount());

        return R.ok(result);
    }

    /**
     * 批量上传教练相关图片（通用接口）
     *
     * @param coachUserId 教练用户ID（从请求头获取）
     * @param files 图片文件数组
     * @return 上传结果
     */
    @PostMapping("/images")
    public R<BatchUploadResultVo> batchUploadImages(
            @RequestHeader(HEADER_USER_ID) Long coachUserId,
            @RequestParam("files") MultipartFile[] files) {

        log.info("开始批量上传图片 - 教练: {}, 文件数量: {}",
                coachUserId, files != null ? files.length : 0);

        // 批量上传文件（使用通用图片类型）
        BatchUploadResultVo result = coachFileUploadService.batchUploadFiles(files, FileTypeEnum.COACH_IMAGE);

        log.info("批量图片上传完成 - 教练: {}, 成功: {}, 失败: {}",
                coachUserId, result.getSuccessCount(), result.getFailureCount());

        return R.ok(result);
    }

}
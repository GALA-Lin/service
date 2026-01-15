package com.unlimited.sports.globox.venue.admin.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.cos.vo.BatchUploadResultVo;
import com.unlimited.sports.globox.venue.admin.dto.CreateVenueInitDto;
import com.unlimited.sports.globox.venue.admin.service.IVenueInitService;
import com.unlimited.sports.globox.venue.admin.vo.VenueInitResultVo;
import com.unlimited.sports.globox.venue.service.IFileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

/**
 * 场馆初始化管理接口（内部接口）
 * todo 作为临时内部插入数据快捷接口,后续可移至管理后台接口
 */
@Slf4j
@RestController
@RequestMapping("/admin/venue/init")
@Tag(name = "场馆初始化管理", description = "内部一键式初始化场馆接口")
public class VenueInitController {

    @Autowired
    private IVenueInitService venueInitService;

    @Autowired
    private IFileUploadService fileUploadService;

    /**
     * 上传场馆图片（批量）
     *
     * @param files 场馆图片文件数组
     * @return 批量上传结果（成功/失败列表）
     */
    @PostMapping("/images/upload")
    @Operation(summary = "批量上传场馆图片",
            description = "批量上传场馆图片，返回上传成功和失败的详细信息。成功的图片URL可用于一键创建场馆")
    public R<BatchUploadResultVo> uploadVenueImages(
            @RequestParam("files") MultipartFile[] files) {

        log.info("收到场馆图片批量上传请求，文件数量：{}", files != null ? files.length : 0);
        BatchUploadResultVo result = fileUploadService.batchUploadFiles(files, FileTypeEnum.VENUE_IMAGE);
        log.info("场馆图片批量上传完成，成功{}张，失败{}张", result.getSuccessCount(), result.getFailureCount());
        return R.ok(result);
    }

    /**
     * 批量上传文件（通用接口）
     *
     * @param files 文件数组
     * @param fileType 文件类型（VENUE_IMAGE、MERCHANT_LICENSE等）
     * @return 批量上传结果
     */
    @PostMapping("/batch-upload")
    @Operation(summary = "批量上传文件（通用）",
            description = "通用的批量文件上传接口，支持多种文件类型。返回上传成功和失败的详细信息")
    public R<BatchUploadResultVo> batchUploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "fileType", defaultValue = "VENUE_IMAGE") String fileType) {

        log.info("收到批量文件上传请求，文件类型：{}，文件数量：{}", fileType, files != null ? files.length : 0);

        FileTypeEnum fileTypeEnum = FileTypeEnum.valueOf(fileType);
        BatchUploadResultVo result = fileUploadService.batchUploadFiles(files, fileTypeEnum);
        log.info("批量文件上传完成，成功{}个，失败{}个", result.getSuccessCount(), result.getFailureCount());
        return R.ok(result);
    }

    /**
     * 一键式创建场馆
     *
     * @param dto 场馆配置信息（包含商家ID、图片URL列表）
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(summary = "一键创建场馆",
            description = "创建场馆及其所有相关配置（场地、营业时间、价格、槽位、设施等）")
    public R<VenueInitResultVo> createVenue(
            @Valid @RequestBody CreateVenueInitDto dto) {

        log.info("收到创建场馆请求：merchantId={}, venueName={}, courtCount={}, imageCount={}",
                dto.getMerchantId(), dto.getVenueBasicInfo().getName(),
                dto.getCourts().size(), dto.getImageUrls() != null ? dto.getImageUrls().size() : 0);
        VenueInitResultVo result = venueInitService.createVenue(dto.getMerchantId(), dto);
        log.info("场馆创建成功：venueId={}", result.getVenueId());
        return R.ok(result);
    }
}

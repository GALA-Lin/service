package com.unlimited.sports.globox.merchant.controller;

import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.cos.vo.BatchUploadResultVo;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.venue.vo.FileUploadVo;
import com.unlimited.sports.globox.venue.service.IFileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_EMPLOYEE_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ROLE;

/**
 * @since 2026/1/16 14:33
 *
 */
@Slf4j
@RestController
@RequestMapping("/merchant/file")
@RequiredArgsConstructor
@Validated
public class MerchantUploadFileController {

    private final MerchantAuthUtil merchantAuthUtil;

    @Autowired
    private IFileUploadService fileUploadService;

    @PostMapping("/image")
    public R<BatchUploadResultVo> batchUploadImages(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestParam("files") MultipartFile[] files) {

        // 权限验证
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        log.info("开始批量上传图片 - 操作人: {}, 商家: {}, 文件数量: {}",
                context.getEmployeeId(), context.getMerchantId(),
                files != null ? files.length : 0);

        // 批量上传文件
        BatchUploadResultVo result = fileUploadService.batchUploadFiles(files, FileTypeEnum.ACTIVITY_IMAGE);

        log.info("批量图片上传完成 - 操作人: {}, 成功: {}, 失败: {}",
                context.getEmployeeId(), result.getSuccessCount(), result.getFailureCount());

        return R.ok(result);
    }

}
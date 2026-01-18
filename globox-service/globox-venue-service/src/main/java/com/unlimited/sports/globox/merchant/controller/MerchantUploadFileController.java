package com.unlimited.sports.globox.merchant.controller;

import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.venue.vo.FileUploadVo;
import com.unlimited.sports.globox.venue.service.IFileUploadService;
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
    public R<FileUploadVo> MerchantUploadImage(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestParam("file") MultipartFile file) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        log.info("开始上传图片，文件名: {}, 大小: {} bytes, {}操作人 , {}商家",
                file.getOriginalFilename(), file.getSize(),context.getEmployeeId(), context.getMerchantId());

        String fileUrl = fileUploadService.uploadFile(file, FileTypeEnum.ACTIVITY_IMAGE);

        FileUploadVo vo = new FileUploadVo(
                fileUrl,
                file.getOriginalFilename(),
                file.getSize()
        );

        log.info("图片上传成功: {}", fileUrl);
        return R.ok(vo);
    }

}
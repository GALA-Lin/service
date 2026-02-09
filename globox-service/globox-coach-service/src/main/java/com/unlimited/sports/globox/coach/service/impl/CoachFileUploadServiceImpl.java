package com.unlimited.sports.globox.coach.service.impl;

import com.qcloud.cos.COSClient;
import com.unlimited.sports.globox.coach.service.ICoachFileUploadService;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.prop.CosProperties;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.cos.CosFileUploadUtil;
import com.unlimited.sports.globox.cos.vo.BatchUploadResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class CoachFileUploadServiceImpl implements ICoachFileUploadService {

    @Autowired
    private COSClient cosClient;

    @Autowired
    private CosProperties cosProperties;

    @Override
    public String uploadFile(MultipartFile file, FileTypeEnum fileType) {
        log.info("教练上传文件 - 类型: {} 大小：{}  限制大小：{} ",
                file.getOriginalFilename(), file.getSize(), fileType.getDefaultMaxSize());
        // 校验默认大小限制
        checkFileSize(file, fileType.getDefaultMaxSize());

        log.info("教练上传文件 - 文件名: {}, 类型: {}",
                file.getOriginalFilename(), fileType.getDescription());
        return CosFileUploadUtil.uploadFile(cosClient, cosProperties, file, fileType);
    }

    @Override
    public BatchUploadResultVo batchUploadFiles(MultipartFile[] files, FileTypeEnum fileType) {
        log.info("教练批量上传文件 -  限制大小：{} ",
                fileType.getDefaultMaxSize());
        if (files != null) {
            for (MultipartFile file : files) {
                // 批量上传校验
                log.info("教练批量上传文件 - 文件名: {}, 大小：{}   类型: {}",
                        file.getOriginalFilename(), file.getSize() , fileType.getDescription());
                checkFileSize(file, fileType.getDefaultMaxSize());
            }
        }

        log.info("教练批量上传文件 - 数量: {}, 类型: {}",
                files != null ? files.length : 0, fileType.getDescription());
        return CosFileUploadUtil.batchUploadFiles(cosClient, cosProperties, files, fileType);
    }

    @Override
    public String uploadFile(MultipartFile file, FileTypeEnum fileType, Long maxSize) {
        // 使用自定义限制校验
        checkFileSize(file, maxSize != null ? maxSize : fileType.getDefaultMaxSize());

        log.info("教练上传文件（自定义限制） - 文件名: {}, 类型: {}, 最大: {}MB",
                file.getOriginalFilename(),
                fileType.getDescription(),
                maxSize != null ? maxSize / 1024 / 1024 : "默认");
        return CosFileUploadUtil.uploadFile(cosClient, cosProperties, file, fileType, maxSize);
    }

    /**
     * 统一大小校验方法
     */
    private void checkFileSize(MultipartFile file, Long limitBytes) {
        if (file == null || file.isEmpty()) {
            return;
        }
        if (limitBytes != null && file.getSize() > limitBytes) {
            // 计算友好显示的大小（MB）
            String sizeLimitDesc = (limitBytes / 1024 / 1024) + "MB";
            // 抛出带参数的异常
            throw new GloboxApplicationException(SocialCode.NOTE_UPLOAD_FILE_TOO_LARGE, sizeLimitDesc);
        }
    }
}
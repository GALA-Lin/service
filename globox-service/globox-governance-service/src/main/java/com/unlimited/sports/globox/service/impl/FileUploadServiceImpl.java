package com.unlimited.sports.globox.service.impl;

import com.qcloud.cos.COSClient;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.prop.CosProperties;
import com.unlimited.sports.globox.cos.CosFileUploadUtil;
import com.unlimited.sports.globox.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传
 */
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private COSClient cosClient;

    @Autowired
    private CosProperties cosProperties;

    @Override
    public String uploadFile(MultipartFile file, FileTypeEnum fileType) {
        return CosFileUploadUtil.uploadFile(cosClient, cosProperties, file, fileType);
    }
}

package com.unlimited.sports.globox.venue.service.impl;

import com.qcloud.cos.COSClient;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.prop.CosProperties;
import com.unlimited.sports.globox.cos.CosFileUploadUtil;
import com.unlimited.sports.globox.cos.vo.BatchUploadResultVo;
import com.unlimited.sports.globox.venue.service.IFileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务实现类
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements IFileUploadService {

    @Autowired
    private COSClient cosClient;

    @Autowired
    private CosProperties cosProperties;

    @Override
    public String uploadFile(MultipartFile file, FileTypeEnum fileType) {
        return CosFileUploadUtil.uploadFile(cosClient, cosProperties, file, fileType);
    }

    @Override
    public BatchUploadResultVo batchUploadFiles(MultipartFile[] files, FileTypeEnum fileType) {
        return CosFileUploadUtil.batchUploadFiles(cosClient, cosProperties, files, fileType);
    }
}

package com.unlimited.sports.globox.service;

import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传
 */
public interface FileUploadService {

    String uploadFile(MultipartFile file, FileTypeEnum fileType);

}

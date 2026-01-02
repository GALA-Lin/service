package com.unlimited.sports.globox.social.service;

import com.unlimited.sports.globox.model.social.vo.MediaUploadVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 */
public interface FileUploadService {

    /**
     * 上传文件到腾讯云COS
     *
     * @param file 文件
     * @return 上传结果
     */
    MediaUploadVo uploadFile(MultipartFile file);
}


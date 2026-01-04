package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.cos.vo.BatchUploadResultVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 */
public interface IFileUploadService {

    /**
     * 上传文件到腾讯云COS（使用默认大小限制）
     *
     * @param file     文件
     * @param fileType 文件类型枚举
     * @return 文件URL
     */
    String uploadFile(MultipartFile file, FileTypeEnum fileType);

    /**
     * 批量上传文件到腾讯云COS
     *
     * @param files    文件数组
     * @param fileType 文件类型枚举
     * @return 批量上传结果（包含成功/失败的文件列表）
     */
    BatchUploadResultVo batchUploadFiles(MultipartFile[] files, FileTypeEnum fileType);

}

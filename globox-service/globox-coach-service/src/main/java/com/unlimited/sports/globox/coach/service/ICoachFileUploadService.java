package com.unlimited.sports.globox.coach.service;

import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.cos.vo.BatchUploadResultVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 教练文件上传服务接口
 *
 * @since 2026/02/09
 */
public interface ICoachFileUploadService {

    /**
     * 上传单个文件到腾讯云COS
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

    /**
     * 上传单个文件到腾讯云COS（带自定义大小限制）
     *
     * @param file     文件
     * @param fileType 文件类型枚举
     * @param maxSize  最大文件大小（字节），null表示使用默认限制
     * @return 文件URL
     */
    String uploadFile(MultipartFile file, FileTypeEnum fileType, Long maxSize);
}
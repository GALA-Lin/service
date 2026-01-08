package com.unlimited.sports.globox.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.prop.CosProperties;
import com.unlimited.sports.globox.common.utils.FilePathUtil;
import com.unlimited.sports.globox.common.utils.FileValidationUtil;
import com.unlimited.sports.globox.cos.vo.BatchUploadResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 腾讯云COS文件上传工具类
 */
@Slf4j
public class CosFileUploadUtil {

    /**
     * 上传单个文件到COS
     *
     * @param cosClient 腾讯云COS客户端
     * @param cosProperties COS配置
     * @param file 要上传的文件
     * @param fileType 文件类型
     * @return 文件URL
     */
    public static String uploadFile(COSClient cosClient, CosProperties cosProperties,
                                    MultipartFile file, FileTypeEnum fileType) {
        return uploadFile(cosClient, cosProperties, file, fileType, null);
    }

    /**
     * 批量上传文件到COS
     *
     * @param cosClient 腾讯云COS客户端
     * @param cosProperties COS配置
     * @param files 文件数组
     * @param fileType 文件类型
     * @return 批量上传结果
     */
    public static BatchUploadResultVo batchUploadFiles(COSClient cosClient, CosProperties cosProperties,
                                                       MultipartFile[] files, FileTypeEnum fileType) {
        List<String> successUrls = new ArrayList<>();
        List<BatchUploadResultVo.FailedFileInfo> failedFiles = new ArrayList<>();

        if (files == null || files.length == 0) {
            return BatchUploadResultVo.builder()
                    .successUrls(successUrls)
                    .failedFiles(failedFiles)
                    .successCount(0)
                    .failureCount(0)
                    .totalCount(0)
                    .build();
        }

        int totalCount = files.length;

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            try {
                String url = uploadFile(cosClient, cosProperties, file, fileType);
                successUrls.add(url);
                log.debug("文件上传成功: {}", file.getOriginalFilename());
            } catch (Exception e) {
                log.error("文件上传失败: {}", file.getOriginalFilename(), e);
                failedFiles.add(BatchUploadResultVo.FailedFileInfo.builder()
                        .fileName(file.getOriginalFilename())
                        .reason(e.getMessage())
                        .build());
            }
        }

        int successCount = successUrls.size();
        int failureCount = failedFiles.size();

        log.info("批量文件上传完成: 成功{}个, 失败{}个, 总计{}个",
                successCount, failureCount, totalCount);

        return BatchUploadResultVo.builder()
                .successUrls(successUrls)
                .failedFiles(failedFiles)
                .successCount(successCount)
                .failureCount(failureCount)
                .totalCount(totalCount)
                .build();
    }

    /**
     * 上传单个文件到COS（支持自定义大小限制）
     *
     * @param cosClient 腾讯云COS客户端
     * @param cosProperties COS配置
     * @param file 要上传的文件
     * @param fileType 文件类型
     * @param maxSize 最大文件大小（字节），null表示使用默认限制
     * @return 文件URL
     */
    public static String uploadFile(COSClient cosClient, CosProperties cosProperties,
                                    MultipartFile file, FileTypeEnum fileType, Long maxSize) {
        // 验证文件不为空
        if (file == null || file.isEmpty()) {
            throw new GloboxApplicationException("文件不能为空");
        }

        // 使用工具类验证文件
        FileValidationUtil.validateFile(file.getOriginalFilename(), file.getSize(), fileType, maxSize);

        // 生成文件路径
        String filePath = FilePathUtil.generateFilePath(
                file.getOriginalFilename(),
                fileType,
                cosProperties.getPathPrefix()
        );

        // 上传文件到COS
        try {
            uploadToCos(cosClient, cosProperties, file.getInputStream(), filePath,
                       file.getContentType(), file.getSize());
        } catch (IOException e) {
            log.error("读取文件流失败", e);
            throw new GloboxApplicationException("文件上传失败");
        }

        // 返回文件URL
        String fileUrl = FilePathUtil.buildFileUrl(
                filePath,
                cosProperties.getDomain(),
                cosProperties.getBucketName(),
                cosProperties.getRegion()
        );

        log.info("文件上传完成: {}", fileUrl);
        return fileUrl;
    }

    /**
     * 上传文件到COS
     *
     * @param cosClient COS客户端
     * @param cosProperties COS配置
     * @param inputStream 文件输入流
     * @param key 文件存储路径
     * @param contentType 文件MIME类型
     * @param size 文件大小
     */
    public static void uploadToCos(COSClient cosClient, CosProperties cosProperties,
                                   InputStream inputStream, String key, String contentType, long size) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(size);
            metadata.setContentType(contentType);

            log.debug("上传到COS存储桶: {}, key: {}", cosProperties.getBucketName(), key);

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    cosProperties.getBucketName(),
                    key,
                    inputStream,
                    metadata
            );

            PutObjectResult result = cosClient.putObject(putObjectRequest);
            log.info("文件上传到COS成功: {}, ETag: {}", key, result.getETag());
        } catch (CosClientException e) {
            log.error("文件上传到COS失败: {}", key, e);
            throw new GloboxApplicationException("文件上传失败: " + e.getMessage());
        }
    }
}

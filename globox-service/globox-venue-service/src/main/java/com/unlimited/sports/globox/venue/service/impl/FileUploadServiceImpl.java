package com.unlimited.sports.globox.venue.service.impl;

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
import com.unlimited.sports.globox.venue.service.IFileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

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
        return uploadFile(file, fileType, null);
    }

    private String uploadFile(MultipartFile file, FileTypeEnum fileType, Long maxSize) {
        //  验证文件不为空
        if (file == null || file.isEmpty()) {
            throw new GloboxApplicationException("文件不能为空");
        }

        //  使用工具类验证文件
        FileValidationUtil.validateFile(file.getOriginalFilename(), file.getSize(), fileType, maxSize);

        //  生成文件路径
        String filePath = FilePathUtil.generateFilePath(
                file.getOriginalFilename(),
                fileType,
                cosProperties.getPathPrefix()
        );

        // 上传文件到COS
        try {
            uploadToCos(file.getInputStream(), filePath, file.getContentType(), file.getSize());
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
     * @param inputStream 文件输入流
     * @param key         文件存储路径
     * @param contentType 文件MIME类型
     * @param size        文件大小
     */
    private void uploadToCos(InputStream inputStream, String key, String contentType, long size) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(size);
            metadata.setContentType(contentType);
            log.info("+++++++存储桶{}",cosProperties.getBucketName());

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
            throw new GloboxApplicationException("文件上传失败");
        }
    }
}

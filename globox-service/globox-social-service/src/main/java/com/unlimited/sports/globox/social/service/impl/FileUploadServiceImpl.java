package com.unlimited.sports.globox.social.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.prop.CosProperties;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.common.utils.FilePathUtil;
import com.unlimited.sports.globox.common.utils.FileValidationUtil;
import com.unlimited.sports.globox.model.social.vo.MediaUploadVo;
import com.unlimited.sports.globox.social.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件上传服务实现类
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private COSClient cosClient;

    @Autowired
    private CosProperties cosProperties;

    @Override
    public MediaUploadVo uploadFile(MultipartFile file) {
        // 1. 验证文件不为空
        if (file == null || file.isEmpty()) {
            throw new GloboxApplicationException(SocialCode.NOTE_UPLOAD_FILE_FAILED);
        }

        String originalFileName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFileName)) {
            throw new GloboxApplicationException(SocialCode.NOTE_UPLOAD_FILE_FAILED);
        }

        long fileSize = file.getSize();
        if (fileSize <= 0) {
            throw new GloboxApplicationException(SocialCode.NOTE_UPLOAD_FILE_FAILED);
        }

        // 2. 根据文件扩展名判断文件类型
        String extension = FileValidationUtil.getFileExtension(originalFileName);
        FileTypeEnum fileType;
        if (FileTypeEnum.SOCIAL_NOTE_IMAGE.isExtensionAllowed(extension)) {
            fileType = FileTypeEnum.SOCIAL_NOTE_IMAGE;
        } else if (FileTypeEnum.SOCIAL_NOTE_VIDEO.isExtensionAllowed(extension)) {
            fileType = FileTypeEnum.SOCIAL_NOTE_VIDEO;
        } else {
            throw new GloboxApplicationException(SocialCode.NOTE_UPLOAD_FILE_TYPE_NOT_SUPPORTED);
        }

        // 3. 校验文件大小
        if (fileSize > fileType.getDefaultMaxSize()) {
            throw new GloboxApplicationException(SocialCode.NOTE_UPLOAD_FILE_TOO_LARGE);
        }

        // 4. 生成文件路径
        String filePath = FilePathUtil.generateFilePath(
                originalFileName,
                fileType,
                cosProperties.getPathPrefix()
        );

        // 5. 上传文件到COS
        try {
            uploadToCos(file.getInputStream(), filePath, file.getContentType(), file.getSize());
        } catch (IOException e) {
            log.error("读取文件流失败", e);
            throw new GloboxApplicationException(SocialCode.NOTE_UPLOAD_FILE_FAILED);
        }

        // 6. 构建文件URL
        String fileUrl = FilePathUtil.buildFileUrl(
                filePath,
                cosProperties.getDomain(),
                cosProperties.getBucketName(),
                cosProperties.getRegion()
        );

        log.info("文件上传完成: {}", fileUrl);

        // 7. 构建返回结果
        MediaUploadVo result = new MediaUploadVo();
        result.setUrl(fileUrl);
        result.setFileName(originalFileName);
        result.setSize(fileSize);

        return result;
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
            throw new GloboxApplicationException(SocialCode.NOTE_UPLOAD_FILE_FAILED);
        }
    }
}


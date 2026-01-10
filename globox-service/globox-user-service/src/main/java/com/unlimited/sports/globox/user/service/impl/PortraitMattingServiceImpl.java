package com.unlimited.sports.globox.user.service.impl;

import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.prop.CosProperties;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.common.utils.FileValidationUtil;
import com.unlimited.sports.globox.common.utils.NotificationSender;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import com.unlimited.sports.globox.user.client.PortraitMattingClient;
import com.unlimited.sports.globox.user.mapper.UserProfileMapper;
import com.unlimited.sports.globox.user.service.PortraitMattingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 球星卡肖像抠图服务实现
 * 集成腾讯云数据万象 CI - 人像抠图功能
 * 使用 PortraitMattingClient (RestTemplate + COS 签名机制)
 */
@Service
@Slf4j
public class PortraitMattingServiceImpl implements PortraitMattingService {

    @Autowired
    private PortraitMattingClient portraitMattingClient;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private NotificationSender notificationSender;

    /**
     * 上传球星卡肖像并执行抠图
     * 使用上传时处理方式（Pic-Operations）
     *
     * @param file 用户上传的图片文件
     * @param userId 用户 ID
     * @return 抠图后图片的 URL
     * @throws Exception 上传或抠图异常
     */
    @Override
    public String uploadAndMatting(MultipartFile file, Long userId) throws Exception {
        try {
            // 1. 文件基础校验
            validateFile(file);

            // 2. 生成文件路径
            String filePath = generateFilePath(file.getOriginalFilename());

            // 3. 获取文件字节内容
            byte[] fileContent = file.getBytes();

            // 4. 调用客户端执行上传并抠图
            String mattingUrl = portraitMattingClient.uploadAndMatting(filePath, fileContent);

            log.info("球星卡肖像抠图成功: userId={}, mattingUrl={}", userId, mattingUrl);
            return mattingUrl;

        } catch (GloboxApplicationException e) {
            log.error("球星卡肖像处理业务异常: userId={}, {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("球星卡肖像处理异常: userId={}", userId, e);
            throw new GloboxApplicationException(UserAuthCode.PORTRAIT_MATTING_FAILED);
        }
    }

    /**
     * 生成 COS 文件路径
     *
     * @param originalFileName 原始文件名
     * @return COS 文件路径
     */
    private String generateFilePath(String originalFileName) {
        String extension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
        return String.format("star-card-portrait/%d/%s%s",
                System.currentTimeMillis(),
                UUID.randomUUID(),
                extension);
    }

    /**
     * 验证上传文件
     *
     * @param file 上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GloboxApplicationException(UserAuthCode.MISSING_UPLOAD_FILE);
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new GloboxApplicationException(UserAuthCode.MISSING_UPLOAD_FILE);
        }

        long fileSize = file.getSize();
        if (fileSize <= 0) {
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
        }

        long maxSize = FileTypeEnum.STAR_CARD_PORTRAIT.getDefaultMaxSize();
        if (fileSize > maxSize) {
            throw new GloboxApplicationException(UserAuthCode.UPLOAD_FILE_TOO_LARGE);
        }

        String extension = FileValidationUtil.getFileExtension(originalFileName);
        if (!FileTypeEnum.STAR_CARD_PORTRAIT.isExtensionAllowed(extension)) {
            throw new GloboxApplicationException(UserAuthCode.UPLOAD_FILE_TYPE_NOT_SUPPORTED);
        }
    }

    /**
     * 异步处理人像抠图并更新用户资料
     *
     * @param userId 用户ID
     * @param fileContent 文件内容（字节数组）
     * @param originalFilename 原始文件名
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void processAsync(Long userId, byte[] fileContent, String originalFilename) {
        try {
            log.info("开始异步处理球星卡肖像抠图: userId={}, filename={}, fileSize={}",
                    userId, originalFilename, fileContent.length);

            // 文件验证
            validateFileContent(fileContent, originalFilename);

            // 生成文件路径
            String filePath = generateFilePath(originalFilename);

            // 调用客户端执行上传并抠图
            String mattingUrl = portraitMattingClient.uploadAndMatting(filePath, fileContent);

            // 更新用户资料数据库
            UserProfile profile = userProfileMapper.selectById(userId);
            if (profile != null) {
                profile.setPortraitUrl(mattingUrl);
                userProfileMapper.updateById(profile);
                log.info("球星卡肖像异步处理成功: userId={}, portraitUrl={}", userId, mattingUrl);

                // 发送通知
                try {
                    notificationSender.sendNotification(
                            userId,
                            NotificationEventEnum.SYSTEM_PORTRAIT_MATTING_COMPLETED,
                            userId,
                            NotificationSender.createCustomData()
                                    .put("portraitUrl", mattingUrl)
                                    .build()
                    );
                    log.info("球星卡肖像处理完成通知已发送: userId={}", userId);
                } catch (Exception e) {
                    log.error("发送球星卡肖像处理完成通知失败: userId={}", userId, e);
                    // 通知发送失败不影响主流程
                }
            } else {
                log.error("球星卡肖像异步处理失败，用户资料不存在: userId={}", userId);
            }

        } catch (Exception e) {
            log.error("球星卡肖像异步处理异常: userId={}", userId, e);
        }
    }

    /**
     * 验证文件内容（用于异步处理）
     *
     * @param fileContent 文件内容
     * @param originalFilename 原始文件名
     */
    private void validateFileContent(byte[] fileContent, String originalFilename) {
        if (fileContent == null || fileContent.length == 0) {
            throw new GloboxApplicationException(UserAuthCode.MISSING_UPLOAD_FILE);
        }

        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new GloboxApplicationException(UserAuthCode.MISSING_UPLOAD_FILE);
        }

        long maxSize = FileTypeEnum.STAR_CARD_PORTRAIT.getDefaultMaxSize();
        if (fileContent.length > maxSize) {
            throw new GloboxApplicationException(UserAuthCode.UPLOAD_FILE_TOO_LARGE);
        }

        String extension = FileValidationUtil.getFileExtension(originalFilename);
        if (!FileTypeEnum.STAR_CARD_PORTRAIT.isExtensionAllowed(extension)) {
            throw new GloboxApplicationException(UserAuthCode.UPLOAD_FILE_TYPE_NOT_SUPPORTED);
        }
    }
}

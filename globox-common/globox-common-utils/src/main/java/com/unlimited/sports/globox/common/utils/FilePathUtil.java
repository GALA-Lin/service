package com.unlimited.sports.globox.common.utils;

import com.unlimited.sports.globox.common.enums.FileTypeEnum;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件路径生成工具类
 */
public class FilePathUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 生成文件存储路径
     * 格式：{pathPrefix}/{fileType}/{yyyy-MM-dd}/{uuid}.{ext}
     *
     * @param originalFileName 原始文件名
     * @param fileType         文件类型枚举
     * @param pathPrefix       路径前缀
     * @return 生成的文件路径
     */
    public static String generateFilePath(String originalFileName, FileTypeEnum fileType, String pathPrefix) {
        // 获取文件扩展名
        String extension = FileValidationUtil.getFileExtension(originalFileName);

        // 生成UUID
        String uuid = generateUUID();

        // 生成日期路径
        String datePath = LocalDate.now().format(DATE_FORMATTER);

        // 组合文件路径
        if (pathPrefix != null && !pathPrefix.isEmpty()) {
            return String.format("%s/%s/%s/%s%s",
                    pathPrefix,
                    fileType.getFilePath(),
                    datePath,
                    uuid,
                    extension);
        } else {
            return String.format("%s/%s/%s%s",
                    fileType.getFilePath(),
                    datePath,
                    uuid,
                    extension);
        }
    }

    /**
     * 生成文件存储路径（不使用路径前缀）
     *
     * @param originalFileName 原始文件名
     * @param fileType         文件类型枚举
     * @return 生成的文件路径
     */
    public static String generateFilePath(String originalFileName, FileTypeEnum fileType) {
        return generateFilePath(originalFileName, fileType, null);
    }

    /**
     * 生成自定义文件存储路径
     * 格式：{customPath}/{uuid}.{ext}
     *
     * @param originalFileName 原始文件名
     * @param customPath       自定义路径
     * @return 生成的文件路径
     */
    public static String generateCustomFilePath(String originalFileName, String customPath) {
        String extension = FileValidationUtil.getFileExtension(originalFileName);
        String uuid = generateUUID();

        if (customPath != null && !customPath.isEmpty()) {
            // 移除末尾的斜杠
            customPath = customPath.replaceAll("/$", "");
            return String.format("%s/%s%s", customPath, uuid, extension);
        } else {
            return uuid + extension;
        }
    }

    /**
     * 生成UUID（去除连字符）
     *
     * @return 32位UUID字符串
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 从完整路径中提取文件名
     *
     * @param filePath 完整文件路径
     * @return 文件名
     */
    public static String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < filePath.length() - 1) {
            return filePath.substring(lastSlash + 1);
        }
        return filePath;
    }

    /**
     * 从URL中提取文件key
     * 支持两种格式：
     * 1. 自定义域名：https://domain.com/path/to/file.jpg
     * 2. COS域名：https://bucket.cos.region.myqcloud.com/path/to/file.jpg
     *
     * @param fileUrl      文件URL
     * @param customDomain 自定义域名（可选）
     * @return 文件key
     */
    public static String extractKeyFromUrl(String fileUrl, String customDomain) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new IllegalArgumentException("文件URL不能为空");
        }

        // 自定义域名格式
        if (customDomain != null && !customDomain.isEmpty() && fileUrl.contains(customDomain)) {
            int index = fileUrl.indexOf(customDomain) + customDomain.length();
            if (index < fileUrl.length()) {
                String key = fileUrl.substring(index);
                // 移除开头的斜杠
                return key.startsWith("/") ? key.substring(1) : key;
            }
        }

        // COS域名格式
        if (fileUrl.contains(".myqcloud.com/")) {
            int index = fileUrl.indexOf(".myqcloud.com/") + ".myqcloud.com/".length();
            return fileUrl.substring(index);
        }

        throw new IllegalArgumentException("无效的文件URL格式");
    }

    /**
     * 构建文件访问URL
     *
     * @param key          文件key
     * @param customDomain 自定义域名（可选）
     * @param bucketName   存储桶名称
     * @param region       地域
     * @return 文件访问URL
     */
    public static String buildFileUrl(String key, String customDomain, String bucketName, String region) {
        // 如果配置了自定义域名，使用自定义域名
        if (customDomain != null && !customDomain.isEmpty()) {
            // 确保域名以 / 结尾
            String domain = customDomain.endsWith("/") ? customDomain.substring(0, customDomain.length() - 1) : customDomain;
            return domain + "/" + key;
        }

        // 否则使用默认COS域名
        return String.format("https://%s.cos.%s.myqcloud.com/%s", bucketName, region, key);
    }

    /**
     * 规范化路径（移除多余的斜杠）
     *
     * @param path 路径
     * @return 规范化后的路径
     */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // 替换多个连续斜杠为单个斜杠
        return path.replaceAll("/+", "/");
    }
}

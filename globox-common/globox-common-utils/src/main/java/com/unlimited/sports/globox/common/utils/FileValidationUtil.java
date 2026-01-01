package com.unlimited.sports.globox.common.utils;

import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;

/**
 * 文件验证工具类
 */
public class FileValidationUtil {

    /**
     * 验证文件
     *
     * @param fileName 文件名
     * @param fileSize 文件大小（字节）
     * @param fileType 文件类型枚举
     * @throws GloboxApplicationException 验证失败时抛出异常
     */
    public static void validateFile(String fileName, Long fileSize, FileTypeEnum fileType) {
        validateFile(fileName, fileSize, fileType, null);
    }

    /**
     * 验证文件（指定最大大小）
     *
     * @param fileName 文件名
     * @param fileSize 文件大小（字节）
     * @param fileType 文件类型枚举
     * @param maxSize  最大文件大小（字节），null则使用fileType的默认值
     * @throws GloboxApplicationException 验证失败时抛出异常
     */
    public static void validateFile(String fileName, Long fileSize, FileTypeEnum fileType, Long maxSize) {
        //  验证文件名
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new GloboxApplicationException("文件名不能为空");
        }

        //  验证文件大小
        if (fileSize == null || fileSize <= 0) {
            throw new GloboxApplicationException("文件大小无效");
        }

        //  获取有效的最大文件大小
        long effectiveMaxSize = (maxSize != null && maxSize > 0) ? maxSize : fileType.getDefaultMaxSize();

        if (fileSize > effectiveMaxSize) {
            throw new GloboxApplicationException(
                    String.format("文件大小不能超过%s", formatFileSize(effectiveMaxSize))
            );
        }

        //  验证文件扩展名
        String extension = getFileExtension(fileName);
        if (!fileType.isExtensionAllowed(extension)) {
            throw new GloboxApplicationException(
                    String.format("不支持的文件类型，%s仅支持%s格式",
                            fileType.getDescription(),
                            String.join("、", fileType.getAllowedExtensions()))
            );
        }
    }

    /**
     * 获取文件扩展名（包含点号）
     *
     * @param fileName 文件名
     * @return 扩展名，如：.jpg
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小（字节）
     * @return 格式化后的字符串，如：10MB、5KB
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1fKB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1fMB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1fGB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 判断文件是否为图片
     *
     * @param fileName 文件名
     * @return true-是图片，false-不是图片
     */
    public static boolean isImage(String fileName) {
        String extension = getFileExtension(fileName);
        return extension.matches("\\.(jpg|jpeg|png|gif|bmp|webp)");
    }

    /**
     * 判断文件是否为文档
     *
     * @param fileName 文件名
     * @return true-是文档，false-不是文档
     */
    public static boolean isDocument(String fileName) {
        String extension = getFileExtension(fileName);
        return extension.matches("\\.(pdf|doc|docx|xls|xlsx|txt)");
    }

    /**
     * 验证文件名是否安全（防止路径遍历攻击）
     *
     * @param fileName 文件名
     * @return true-安全，false-不安全
     */
    public static boolean isSafeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        // 检查是否包含路径分隔符或其他危险字符
        return !fileName.contains("..") &&
                !fileName.contains("/") &&
                !fileName.contains("\\") &&
                !fileName.contains("\0");
    }
}

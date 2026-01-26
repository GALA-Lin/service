package com.unlimited.sports.globox.user.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 球星卡肖像抠图服务接口
 *
 * 使用腾讯云数据万象 CI - AIPortraitMatting 接口
 * 在上传时自动执行人像抠图
 */
public interface PortraitMattingService {

    /**
     * 上传球星卡肖像并执行抠图
     *
     * 流程：
     * 1. 接收用户上传的图片文件
     * 2. 使用 COS SDK 的 PutObjectRequest 上传文件
     * 3. 通过 ObjectMetadata 设置自定义元数据包含 Pic-Operations 参数
     * 4. COS SDK 自动处理签名认证（会包含所有必要的头）
     * 5. 腾讯云在上传时自动执行人像抠图，生成透明背景 PNG
     * 6. 返回抠图后的图片 URL
     *
     * @param file 用户上传的图片文件（MultipartFile）
     * @param userId 用户 ID（用于生成文件路径）
     * @return 抠图后图片的 URL
     * @throws Exception 上传或抠图过程中的异常
     */
    String uploadAndMatting(MultipartFile file, Long userId) throws Exception;

    /**
     * 异步处理人像抠图并更新用户资料
     *
     * @param userId 用户ID
     * @param fileContent 文件内容（字节数组）
     * @param originalFilename 原始文件名
     */
    void processAsync(Long userId, byte[] fileContent, String originalFilename);
}

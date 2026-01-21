package com.unlimited.sports.globox.user.service;

/**
 * Apple登录服务接口
 */
public interface AppleService {

    /**
     * 验证并解析 Apple identityToken，提取用户唯一标识（sub）
     *
     * @param identityToken Apple identityToken（JWT格式）
     * @return Apple 用户唯一标识（sub）
     */
    String verifyAndExtractSub(String identityToken);
}


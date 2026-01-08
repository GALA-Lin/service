package com.unlimited.sports.globox.user.service;

import com.unlimited.sports.globox.model.auth.entity.InternalTestWhitelist;

/**
 * 白名单服务接口
 *
 * @author Wreckloud
 * @since 2025/12/19
 * @deprecated TODO: 后续将 whitelist 命名更改为 allowlist，黑名单命名为 blocklist
 *             命名规范：白名单统一使用 allowList，黑名单统一使用 blockList
 */
public interface WhitelistService {

    /**
     * 检查手机号是否在白名单内
     *
     * @param phone 手机号
     * @return true=在白名单内，false=不在白名单内
     */
    boolean isInWhitelist(String phone);

    /**
     * 根据手机号查询白名单记录
     *
     * @param phone 手机号
     * @return 白名单记录，不存在则返回null
     */
    InternalTestWhitelist getWhitelistByPhone(String phone);

    /**
     * 添加手机号到白名单
     *
     * @param phone        手机号
     * @param inviteSource 邀请来源
     * @return true=添加成功，false=已存在
     */
    boolean addToWhitelist(String phone, String inviteSource);
}

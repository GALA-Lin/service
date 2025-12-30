package com.unlimited.sports.globox.model.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内测白名单表
 *
 * @author Wreckloud
 * @since 2025/12/19
 * @deprecated TODO: 后续将 whitelist 命名更改为 allowlist，黑名单命名为 blocklist
 *             命名规范：白名单统一使用 allowList，黑名单统一使用 blockList
 */
@Data
@TableName("internal_test_whitelist")
public class InternalTestWhitelist {
    
    /**
     * 白名单表主键（自增ID）
     */
    @TableId(value = "whitelist_id", type = IdType.AUTO)
    private Long whitelistId;
    
    /**
     * 白名单手机号
     */
    private String phone;
    
    /**
     * 邀请来源
     */
    private String inviteSource;
    
    /**
     * 状态
     */
    private WhitelistStatus status;
    
    /**
     * 添加时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 白名单状态枚举
     */
    public enum WhitelistStatus {
        ACTIVE,
        DISABLED
    }
}

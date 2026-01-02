package com.unlimited.sports.globox.model.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户认证主表
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Data
@TableName("auth_user")
public class AuthUser {
    
    /**
     * 用户ID（自增主键）
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;
    
    /**
     * 账号状态
     */
    private UserStatus status;
    
    /**
     * 用户角色
     */
    private UserRole role;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 账号状态枚举
     */
    public enum UserStatus {
        ACTIVE,
        DISABLED
    }
    
    /**
     * 用户角色枚举
     */
    public enum UserRole {
        /**
         * 普通用户
         */
        USER,

        /**
         * 教练
         */
        COACH,

        /**
         * 管理员
         */
        ADMIN
    }
}

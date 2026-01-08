package com.unlimited.sports.globox.model.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录记录表
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Data
@TableName("user_login_records")
public class UserLoginRecord {
    
    /**
     * 记录主键（自增ID）
     */
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;
    
    /**
     * 用户ID（登录失败时可能为null）
     */
    private Long userId;
    
    /**
     * 登录方式
     */
    private AuthIdentity.IdentityType identityType;
    
    /**
     * 脱敏后的标识
     */
    private String identifierMasked;
    
    /**
     * 登录结果
     */
    private LoginResult result;
    
    /**
     * 失败原因码
     */
    private String failReason;
    
    /**
     * 登录IP地址
     */
    private String ip;
    
    /**
     * 设备信息
     */
    private String userAgent;
    
    /**
     * 登录时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 登录结果枚举
     */
    public enum LoginResult {
        SUCCESS,
        FAIL
    }
}

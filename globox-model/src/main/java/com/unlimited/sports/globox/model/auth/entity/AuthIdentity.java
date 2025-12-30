package com.unlimited.sports.globox.model.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录方式绑定表
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Data
@TableName("auth_identity")
public class AuthIdentity {
    
    /**
     * 身份表主键（自增ID）
     */
    @TableId(value = "identity_table_id", type = IdType.AUTO)
    private Long identityTableId;
    
    /**
     * 身份标识业务主键（唯一标识）
     */
    @TableField("identity_id")
    private String identityId;
    
    /**
     * 关联的用户ID
     */
    private Long userId;
    
    /**
     * 登录方式类型
     */
    private IdentityType identityType;
    
    /**
     * 认证载体
     */
    private String identifier;
    
    /**
     * 凭证
     */
    private String credential;
    
    /**
     * 是否已验证
     */
    private Boolean verified;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 登录方式类型枚举
     */
    public enum IdentityType {
        PHONE,
        WECHAT,
        APPLE
    }
}

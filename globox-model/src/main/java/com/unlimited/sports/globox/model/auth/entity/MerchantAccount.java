package com.unlimited.sports.globox.model.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.unlimited.sports.globox.model.auth.enums.MerchantRole;
import com.unlimited.sports.globox.model.auth.enums.MerchantStatus;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家账号实体
 */
@Data
@TableName("merchant_account")
public class MerchantAccount implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 账号ID
     */
    @TableId(value = "account_id", type = IdType.AUTO)
    private Long accountId;

    /**
     * 账号
     */
    private String account;

    /**
     * 密码hash
     */
    private String passwordHash;

    /**
     * 状态
     */
    private MerchantStatus status;

    /**
     * 角色
     */
    private MerchantRole role;


    /**
     * 职工id 对应商家的id或者其下职工的id
     */
    private String employeeId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

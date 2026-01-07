package com.unlimited.sports.globox.model.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.unlimited.sports.globox.model.auth.enums.LoginResult;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家登录记录实体
 */
@Data
@TableName("merchant_login_records")
public class MerchantLoginRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    /**
     * 商家ID
     */
    private Long merchantId;

    /**
     * 登录结果
     */
    private LoginResult result;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

package com.unlimited.sports.globox.model.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**

 * @since 2025-12-18-10:29
 * 商家信息表
 */

@Data
@TableName("merchants")
public class Merchant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商家ID，主键
     */

    @TableId(value = "merchant_id", type = IdType.AUTO)
    private Long merchantId;

    /**
     * 关联的用户ID，一个用户只能是一个商家
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 商家名称
     */
    @TableField("merchant_name")
    private String merchantName;

    /**
     * 商家类型：1=个人，2=公司，....
     */
    @TableField("merchant_type")
    private Integer merchantType;

    /**
     * 联系电话
     */
    @TableField("contact_phone")
    private String contactPhone;

    /**
     * 联系邮箱
     */
    @TableField("contact_email")
    private String contactEmail;

    /**
     * 身份证号或营业执照号
     */
    @TableField("id_number")
    private String idNumber;

    /**
     * 认证状态：0-待审核，1-已认证，2-审核失败
     */
    @TableField("certification_status")
    private Integer certificationStatus;

    /**
     * 认证通过时间
     */
    @TableField("certification_time")
    private LocalDateTime certificationTime;

    /**
     * 银行账号
     */
    @TableField("bank_account")
    private String bankAccount;

    /**
     * 银行名称
     */
    @TableField("bank_name")
    private String bankName;

    /**
     * 账户持有人
     */
    @TableField("account_holder")
    private String accountHolder;

    /**
     * 商家状态：0-禁用，1-正常，2-冻结
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

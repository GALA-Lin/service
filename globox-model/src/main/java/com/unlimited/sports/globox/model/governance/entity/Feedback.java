package com.unlimited.sports.globox.model.governance.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户反馈表
 */
@Data
@Builder
@NotNull
@AllArgsConstructor
@TableName("feedback")
@EqualsAndHashCode(callSuper = true)
public class Feedback extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 联系方式(可选)
     */
    private String contact;

    /**
     * 反馈内容
     */
    private String content;

    /**
     * 客户端类型
     */
    private ClientType clientType;

    /**
     * App版本号
     */
    private String appVersion;

    /**
     * 系统版本
     */
    private String osVersion;

    /**
     * 设备型号
     */
    private String deviceModel;

    /**
     * 客户端IP
     */
    private String ip;
}
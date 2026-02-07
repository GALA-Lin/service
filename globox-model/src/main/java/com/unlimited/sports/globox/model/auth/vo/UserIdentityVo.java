package com.unlimited.sports.globox.model.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户已绑定身份信息
 *
 * @author Wreckloud
 * @since 2026/02/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户已绑定身份信息")
public class UserIdentityVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "身份类型：0=WECHAT，1=APPLE，2=PHONE", example = "0")
    private Integer identityType;

    @Schema(description = "手机号（identityType=PHONE时返回）", example = "13800138000")
    private String phone;

    @Schema(description = "是否已验证", example = "true")
    private Boolean verified;
}

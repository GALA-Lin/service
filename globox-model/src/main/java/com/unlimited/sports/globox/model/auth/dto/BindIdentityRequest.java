package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 已登录用户绑定第三方身份请求
 *
 */
@Data
@Schema(description = "已登录用户绑定第三方身份请求")
public class BindIdentityRequest {

    @NotNull(message = "identityType不能为空")
    @Schema(description = "身份类型：0=WECHAT，1=APPLE，2=PHONE", example = "0")
    private Integer identityType;

    @Schema(description = "登录code（identityType=WECHAT时为微信code；identityType=APPLE时为identityToken；identityType=PHONE时为短信验证码）", example = "wxcode123")
    private String code;

    @Schema(description = "手机号（identityType=PHONE时必填）", example = "13800138000")
    private String phone;

    public enum IdentityType {
        WECHAT(0),
        APPLE(1),
        PHONE(2);

        private final int code;

        IdentityType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static IdentityType fromCode(Integer code) {
            if (code == null) {
                return null;
            }
            for (IdentityType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return null;
        }
    }
}

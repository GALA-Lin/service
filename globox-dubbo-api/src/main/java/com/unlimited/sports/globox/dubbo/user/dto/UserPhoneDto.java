package com.unlimited.sports.globox.dubbo.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户手机号 DTO（RPC 返回）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPhoneDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    /**
     * 明文手机号
     */
    private String phone;
}

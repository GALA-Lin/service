package com.unlimited.sports.globox.model.demo.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 用户响应参数
 *
 * @author dk
 * @since 2025/12/17 21:43
 */
@Data
public class UserResponseVO {
    private Long id;

    /**
     * 用户名（唯一）
     */
    private String username;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 用户状态：1-正常 0-禁用
     */
    private Integer status;
}

package com.unlimited.sports.globox.model.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListItemVo {

    private Long userId;

    private String globoxNo;

    private String nickName;

    private String avatarUrl;

    /**
     * 性别: 0=女, 1=男
     */
    private Integer gender;

    /**
     * 网球水平 NTRP
     */
    private BigDecimal ntrp;

    /**
     * 粉丝数量（从社交服务获取）
     */
    private Integer followers;
}

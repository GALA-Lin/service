package com.unlimited.sports.globox.venue.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 第三方平台认证信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartyAuthInfo implements Serializable {

    /**
     * Token
     */
    private String token;

    /**
     * 管理员ID
     */
    private String adminId;

    /**
     * 场馆ID
     */
    private String stadiumId;
}

package com.unlimited.sports.globox.venue.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 第三方平台认证信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartyAuthInfo implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /**
     * 商户ID（业务ID）
     */
    private String businessId;

    /**
     * 球场项目ID列表（Wefitos平台专用，存储时段ID）
     */
    private List<String> courtProjectIds;
}

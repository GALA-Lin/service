package com.unlimited.sports.globox.dubbo.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户信息DTO（RPC专用出参）
 * 包含用户基本信息：头像、昵称、性别等
 */
@Data
public class UserInfoDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户头像URL
     */
    private String avatarUrl;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 性别：MALE/FEMALE/OTHER
     */
    private String gender;

    /**
     * 用户网球水平（NTRP）
     */
    private Double userNtrpLevel;
}

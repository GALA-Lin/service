package com.unlimited.sports.globox.model.auth.vo;

import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户信息VO（用于RPC返回）
 * 包含用户基本信息：头像、昵称、性别等
 *
 * @author Wreckloud
 * @since 2025/12/20
 */
@Data
public class UserInfoVo implements Serializable {

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
     * 性别
     */
    private UserProfile.Gender gender;
}

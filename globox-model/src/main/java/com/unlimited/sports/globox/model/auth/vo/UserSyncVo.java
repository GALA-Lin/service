package com.unlimited.sports.globox.model.auth.vo;

import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户同步数据VO
 * 用于搜索服务的用户数据同步
 * 只包含搜索和展示必要的字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSyncVo implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 球盒号（9位数字）
     */
    private String globoxNo;

    /**
     * 头像URL
     */
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
     * 是否已注销
     */
    private Boolean cancelled;

    public static UserSyncVo convertToSyncVo(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        return UserSyncVo.builder()
                .userId(profile.getUserId())
                .nickName(profile.getNickName())
                .globoxNo(profile.getGloboxNo())
                .avatarUrl(profile.getAvatarUrl())
                .gender(profile.getGender() != null ? profile.getGender().getCode() : null)
                .ntrp(profile.getNtrp())
                .cancelled(profile.getCancelled())
                .build();
    }
}

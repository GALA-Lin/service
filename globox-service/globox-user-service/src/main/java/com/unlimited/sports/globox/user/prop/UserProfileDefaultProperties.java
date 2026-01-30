package com.unlimited.sports.globox.user.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 用户默认信息 properties
 */
@Data
@Component
@ConfigurationProperties("user.profile")
public class UserProfileDefaultProperties {
    private String defaultAvatarUrl;
    private String defaultAvatarUrlApp;
    private String defaultAvatarUrlMiniapp;
    private String defaultStarCardMaleUrl;
    private String defaultStarCardFemaleUrl;
}

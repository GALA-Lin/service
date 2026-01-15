package com.unlimited.sports.globox.dubbo.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRelationStatusItemDto implements Serializable {
    private Long targetUserId;
    private Boolean isFollowed;
    private Boolean isMutual;
}

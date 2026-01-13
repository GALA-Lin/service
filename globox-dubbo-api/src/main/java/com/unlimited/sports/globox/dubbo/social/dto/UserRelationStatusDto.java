package com.unlimited.sports.globox.dubbo.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRelationStatusDto {
    private Boolean isFollowed;
    private Boolean isMutual;
}

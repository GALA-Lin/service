package com.unlimited.sports.globox.model.social.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户关系统计
 */
@Data
@Schema(description = "用户关系统计")
public class UserRelationStatsVo {

    @Schema(description = "关注数")
    private Long followCount;

    @Schema(description = "粉丝数")
    private Long fansCount;

    @Schema(description = "获赞数")
    private Long likeCount;
}






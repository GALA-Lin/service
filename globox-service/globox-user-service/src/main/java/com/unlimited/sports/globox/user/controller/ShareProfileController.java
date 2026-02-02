package com.unlimited.sports.globox.user.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.auth.vo.UserProfileVo;
import com.unlimited.sports.globox.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分享页用户资料控制器
 */
@RestController
@RequestMapping("/share/user/profile")
@Tag(name = "分享页用户资料", description = "分享页用户资料查询接口")
public class ShareProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @GetMapping("/{userId}")
    @Operation(summary = "分享页用户资料", description = "获取指定用户的完整资料（含球拍+标签），用于分享页展示")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2010", description = "用户不存在")
    })
    public R<UserProfileVo> getSharedUserProfile(
            @RequestHeader(value = RequestHeaderConstants.HEADER_USER_ID, required = false) Long viewerId,
            @Parameter(description = "目标用户ID", required = true)
            @PathVariable Long userId) {
        return userProfileService.getUserProfile(userId, viewerId);
    }
}



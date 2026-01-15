package com.unlimited.sports.globox.social.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.social.vo.FollowUserVo;
import com.unlimited.sports.globox.model.social.vo.BlockUserVo;
import com.unlimited.sports.globox.model.social.vo.UserRelationStatsVo;
import com.unlimited.sports.globox.social.service.SocialRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/social/relations")
@Tag(name = "关注/拉黑关系", description = "关注、拉黑、列表与统计接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class SocialRelationController {

    @Autowired
    private SocialRelationService socialRelationService;

    @PostMapping("/follow/{userId}")
    @Operation(summary = "关注用户", description = "关注目标用户，幂等；被拉黑或已拉黑时禁止关注")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "关注成功"),
            @ApiResponse(responseCode = "3030", description = "不允许关注自己"),
            @ApiResponse(responseCode = "3032", description = "拉黑状态下无法关注"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> follow(
            @Parameter(description = "当前登录用户ID（由网关注入）")
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "目标用户ID", required = true)
            @PathVariable("userId") Long targetUserId) {
        return socialRelationService.follow(userId, targetUserId);
    }

    @DeleteMapping("/follow/{userId}")
    @Operation(summary = "取消关注", description = "取消关注，幂等")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消关注成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> unfollow(
            @Parameter(description = "当前登录用户ID（由网关注入）")
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "目标用户ID", required = true)
            @PathVariable("userId") Long targetUserId) {
        return socialRelationService.unfollow(userId, targetUserId);
    }

    @PostMapping("/block/{userId}")
    @Operation(summary = "拉黑用户", description = "拉黑后删除双向关注关系，幂等")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "拉黑成功"),
            @ApiResponse(responseCode = "3030", description = "不允许关注/拉黑自己"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> block(
            @Parameter(description = "当前登录用户ID（由网关注入）")
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "目标用户ID", required = true)
            @PathVariable("userId") Long targetUserId) {
        return socialRelationService.block(userId, targetUserId);
    }

    @DeleteMapping("/block/{userId}")
    @Operation(summary = "取消拉黑", description = "取消拉黑，幂等")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消拉黑成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> unblock(
            @Parameter(description = "当前登录用户ID（由网关注入）")
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "目标用户ID", required = true)
            @PathVariable("userId") Long targetUserId) {
        return socialRelationService.unblock(userId, targetUserId);
    }

    @GetMapping("/following")
    @Operation(summary = "关注列表", description = "获取我的关注列表，按关注时间倒序，keyword 仅过滤当前页昵称")
    public R<PaginationResult<FollowUserVo>> getFollowing(
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return socialRelationService.getFollowing(userId, targetUserId, page, pageSize, keyword);
    }

    @GetMapping("/fans")
    @Operation(summary = "粉丝列表", description = "获取我的粉丝列表，按关注时间倒序，keyword 仅过滤当前页昵称")
    public R<PaginationResult<FollowUserVo>> getFans(
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return socialRelationService.getFans(userId, targetUserId, page, pageSize, keyword);
    }

    @GetMapping("/mutual")
    @Operation(summary = "互关列表", description = "获取我的互相关注列表，按关注时间倒序，keyword 仅过滤当前页昵称")
    public R<PaginationResult<FollowUserVo>> getMutual(
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return socialRelationService.getMutual(userId, targetUserId, page, pageSize, keyword);
    }

    @GetMapping("/users/{userId}/stats")
    @Operation(summary = "用户主页统计", description = "获取用户关注数/粉丝数/获赞数")
    public R<UserRelationStatsVo> getUserStats(
            @Parameter(description = "目标用户ID", required = true)
            @PathVariable("userId") Long targetUserId) {
        return socialRelationService.getUserStats(targetUserId);
    }

    @GetMapping("/blocks")
    @Operation(summary = "我已拉黑的用户列表", description = "获取我拉黑的用户，按拉黑时间倒序，支持关键词过滤当前页昵称")
    public R<PaginationResult<BlockUserVo>> getBlockedUsers(
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return socialRelationService.getBlockedUsers(userId, page, pageSize, keyword);
    }
}






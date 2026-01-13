package com.unlimited.sports.globox.user.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.auth.dto.UpdateStarCardPortraitRequest;
import com.unlimited.sports.globox.model.auth.dto.UpdateUserProfileRequest;
import com.unlimited.sports.globox.model.auth.dto.UpdateUserMediaRequest;
import com.unlimited.sports.globox.model.auth.vo.StarCardPortraitVo;
import com.unlimited.sports.globox.model.auth.vo.StarCardVo;
import com.unlimited.sports.globox.model.auth.vo.StyleTagVo;
import com.unlimited.sports.globox.model.auth.vo.ProfileOptionsVo;
import com.unlimited.sports.globox.model.auth.vo.UserProfileVo;
import com.unlimited.sports.globox.model.auth.vo.UserMediaVo;
import com.unlimited.sports.globox.model.venue.vo.FileUploadVo;
import com.unlimited.sports.globox.user.service.UserProfileService;
import com.unlimited.sports.globox.user.service.UserMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户资料控制器
 */
@RestController
@RequestMapping("/user/profile")
@Tag(name = "用户资料模块", description = "用户资料查询、更新、选项、球星卡相关接口")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserMediaService userMediaService;

    @GetMapping
    @Operation(summary = "查询用户资料", description = "获取当前用户的完整资料（含球拍+标签）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2010", description = "用户不存在"),
            @ApiResponse(responseCode = "2049", description = "缺少用户ID请求头"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<UserProfileVo> getUserProfile(
            @Parameter(description = "用户ID（由网关自动注入，测试时可手动设置）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        return userProfileService.getUserProfile(userId);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "按ID查询用户资料", description = "获取指定用户的完整资料（含球拍+标签）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2010", description = "用户不存在"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<UserProfileVo> getUserProfileById(
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long viewerId,
            @Parameter(description = "目标用户ID", required = true)
            @PathVariable Long userId) {
        return userProfileService.getUserProfile(userId, viewerId);
    }

    @PutMapping
    @Operation(summary = "更新用户资料", description = "更新当前用户的资料，只更新非空字段。球拍和标签列表采用完全替换策略（传入空列表会清空）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "2010", description = "用户不存在"),
            @ApiResponse(responseCode = "2049", description = "缺少用户ID请求头"),
            @ApiResponse(responseCode = "2021", description = "无效的Token"),
            @ApiResponse(responseCode = "2040", description = "参数无效"),
            @ApiResponse(responseCode = "2041", description = "球拍型号不存在"),
            @ApiResponse(responseCode = "2042", description = "只能设置一个主力拍"),
            @ApiResponse(responseCode = "2043", description = "球风标签无效或不存在"),
            @ApiResponse(responseCode = "2047", description = "球拍层级无效，必须为MODEL"),
            @ApiResponse(responseCode = "2048", description = "球拍型号已下架或不可用")
    })
    public R<String> updateUserProfile(
            @Parameter(description = "用户ID（由网关自动注入，测试时可手动设置）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "用户资料更新请求", required = true)
            @Validated @RequestBody UpdateUserProfileRequest request) {
        return userProfileService.updateUserProfile(userId, request);
    }

    @GetMapping("/options")
    @Operation(summary = "获取资料可选项", description = "获取球拍字典和球风标签选项（仅返回ACTIVE状态）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<ProfileOptionsVo> getProfileOptions() {
        return userProfileService.getProfileOptions();
    }

    @GetMapping("/star-card")
    @Operation(summary = "获取用户球星卡", description = "获取当前用户的球星卡数据")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2010", description = "用户不存在"),
            @ApiResponse(responseCode = "2049", description = "缺少用户ID请求头"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<StarCardVo> getStarCard(
            @Parameter(description = "用户ID（由网关自动注入，测试时可手动设置）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        return userProfileService.getStarCard(userId);
    }

    @GetMapping("/star-card/portrait")
    @Operation(summary = "获取球星卡肖像", description = "获取当前用户的球星卡肖像URL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2010", description = "用户不存在"),
            @ApiResponse(responseCode = "2049", description = "缺少用户ID请求头"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<StarCardPortraitVo> getStarCardPortrait(
            @Parameter(description = "用户ID（由网关自动注入，测试时可手动设置）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        return userProfileService.getStarCardPortrait(userId);
    }

    @PutMapping("/star-card/portrait")
    @Operation(summary = "更新球星卡肖像",
            description = "更新当前用户的球星卡肖像URL。传入 null 或空字符串表示删除肖像")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "2010", description = "用户不存在"),
            @ApiResponse(responseCode = "2049", description = "缺少用户ID请求头"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> updateStarCardPortrait(
            @Parameter(description = "用户ID（由网关自动注入，测试时可手动设置）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "球星卡肖像更新请求", required = true)
            @Validated @RequestBody UpdateStarCardPortraitRequest request) {
        return userProfileService.updateStarCardPortrait(userId, request);
    }

    @GetMapping("/style-tags")
    @Operation(summary = "获取球风标签列表", description = "获取所有球风标签选项（仅返回ACTIVE状态）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<List<StyleTagVo>> getStyleTags() {
        return userProfileService.getStyleTags();
    }

    @PostMapping("/avatar/upload")
    @Operation(summary = "上传用户头像", description = "上传用户头像图片，支持 jpg/jpeg/png/webp 格式，最大 5MB。上传成功后需要调用 PUT /user/profile 接口，将返回的 url 写入 avatarUrl 字段")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上传成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token"),
            @ApiResponse(responseCode = "2040", description = "参数无效（文件名/大小异常等）"),
            @ApiResponse(responseCode = "2050", description = "缺少上传文件"),
            @ApiResponse(responseCode = "2051", description = "上传文件过大"),
            @ApiResponse(responseCode = "2052", description = "文件类型不支持"),
            @ApiResponse(responseCode = "2053", description = "文件上传失败")
    })
    public R<FileUploadVo> uploadAvatar(
            @Parameter(description = "头像图片文件（jpg/jpeg/png/webp，最大5MB）", required = true)
            @RequestParam("file") MultipartFile file) {
        return userProfileService.uploadAvatar(file);
    }

    @GetMapping("/media")
    @Operation(summary = "查询用户媒体列表", description = "获取当前用户的展示墙媒体列表（照片/视频），支持按类型过滤")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2010", description = "用户不存在"),
            @ApiResponse(responseCode = "2049", description = "缺少用户ID请求头"),
            @ApiResponse(responseCode = "2021", description = "无效的Token"),
            @ApiResponse(responseCode = "2040", description = "参数无效（mediaType格式错误）")
    })
    public R<List<UserMediaVo>> getUserMediaList(
            @Parameter(description = "用户ID（由网关自动注入，测试时可手动设置）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "媒体类型（可选，IMAGE/VIDEO，为空则返回所有类型）", required = false)
            @RequestParam(value = "mediaType", required = false) String mediaType) {
        return userMediaService.getUserMediaList(userId, mediaType);
    }

    @PutMapping("/media")
    @Operation(summary = "保存用户媒体列表", description = "保存用户展示墙媒体列表，采用全量替换策略。传入空列表会清空所有媒体。最大12条（图+视频合计），视频最多5条")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "保存成功"),
            @ApiResponse(responseCode = "2010", description = "用户不存在"),
            @ApiResponse(responseCode = "2049", description = "缺少用户ID请求头"),
            @ApiResponse(responseCode = "2021", description = "无效的Token"),
            @ApiResponse(responseCode = "2040", description = "参数无效"),
            @ApiResponse(responseCode = "2044", description = "媒体数量超过限制，最多12条（图+视频合计）"),
            @ApiResponse(responseCode = "2045", description = "视频数量超过限制，最多5条"),
            @ApiResponse(responseCode = "2046", description = "排序值不能重复"),
            @ApiResponse(responseCode = "2054", description = "媒体地址不能为空"),
            @ApiResponse(responseCode = "2055", description = "视频封面不能为空")
    })
    public R<String> updateUserMedia(
            @Parameter(description = "用户ID（由网关自动注入，测试时可手动设置）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "媒体列表更新请求（全量替换）", required = true)
            @Validated @RequestBody UpdateUserMediaRequest request) {
        return userMediaService.updateUserMedia(userId, request);
    }

    @PostMapping("/media/image/upload")
    @Operation(summary = "上传媒体图片", description = "上传用户展示墙图片，支持 jpg/jpeg/png/webp 格式，最大 10MB。上传成功后需要调用 PUT /user/profile/media 接口，将返回的 url 写入媒体列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上传成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token"),
            @ApiResponse(responseCode = "2040", description = "参数无效（文件名/大小异常等）"),
            @ApiResponse(responseCode = "2050", description = "缺少上传文件"),
            @ApiResponse(responseCode = "2051", description = "上传文件过大"),
            @ApiResponse(responseCode = "2052", description = "文件类型不支持"),
            @ApiResponse(responseCode = "2053", description = "文件上传失败")
    })
    public R<FileUploadVo> uploadMediaImage(
            @Parameter(description = "图片文件（jpg/jpeg/png/webp，最大10MB）", required = true)
            @RequestParam("file") MultipartFile file) {
        return userMediaService.uploadMediaImage(file);
    }

    @PostMapping("/media/video/upload")
    @Operation(summary = "上传媒体视频", description = "上传用户展示墙视频，支持 mp4/mov 格式，最大 100MB。上传成功后需要调用 PUT /user/profile/media 接口，将返回的 url 写入媒体列表。注意：视频需要前端生成封面并上传，coverUrl 由前端保证")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上传成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token"),
            @ApiResponse(responseCode = "2040", description = "参数无效（文件名/大小异常等）"),
            @ApiResponse(responseCode = "2050", description = "缺少上传文件"),
            @ApiResponse(responseCode = "2051", description = "上传文件过大"),
            @ApiResponse(responseCode = "2052", description = "文件类型不支持"),
            @ApiResponse(responseCode = "2053", description = "文件上传失败")
    })
    public R<FileUploadVo> uploadMediaVideo(
            @Parameter(description = "视频文件（mp4/mov，最大100MB）", required = true)
            @RequestParam("file") MultipartFile file) {
        return userMediaService.uploadMediaVideo(file);
    }

    @PostMapping("/star-card/portrait/upload")
    @Operation(summary = "上传球星卡肖像（自动抠图 - 异步处理）")
    public R<String> uploadStarCardPortrait(
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @RequestParam("file") MultipartFile file) {
        return userProfileService.uploadStarCardPortrait(userId, file);
    }

}

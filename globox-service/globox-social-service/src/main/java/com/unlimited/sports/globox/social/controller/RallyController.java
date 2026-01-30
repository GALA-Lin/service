package com.unlimited.sports.globox.social.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;

import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.model.social.dto.*;
import com.unlimited.sports.globox.model.social.entity.*;
import com.unlimited.sports.globox.model.social.vo.RallyApplicationVo;
import com.unlimited.sports.globox.model.social.vo.RallyPostsDetailsVo;
import com.unlimited.sports.globox.model.social.vo.RallyPostsVo;
import com.unlimited.sports.globox.model.social.vo.RallyQueryVo;
import com.unlimited.sports.globox.social.service.RallyService;
import com.unlimited.sports.globox.social.service.impl.RallyServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import javax.validation.Valid;
import java.util.List;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;
import static com.unlimited.sports.globox.common.result.UserAuthCode.TOKEN_EXPIRED;

/**
 * 约球控制器
 **/
@Slf4j
@RestController
@RequestMapping("/rally")
@Tag(name = "约球模块", description = "约球活动创建、查询、申请、审核相关接口")
@SecurityRequirement(name = "bearerAuth")
public class RallyController {

    @Autowired
    private RallyService rallyService;

    /**
     * 获取约球列表
     * @return 约球列表分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "获取约球列表", description = "根据筛选条件获取约球列表，支持分页")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<RallyQueryVo> getRallyList(
            @Parameter(description = "约球查询条件", required = true)
            @Valid RallyQueryDto rallyQueryDto) {
        log.info("获取筛选条件：-------------{}",rallyQueryDto);
        RallyQueryVo rallyPostsVoPaginationResult = rallyService.getRallyPostsList(rallyQueryDto);
        return R.ok(rallyPostsVoPaginationResult);
    }

    /**
     * 获取约球详情
     * 
     * @param postId 约球帖子ID
     * @param rallyApplicantId 申请人ID
     * @return 约球详情
     */
    @GetMapping("/details")
    @Operation(summary = "获取约球详情", description = "获取指定约球活动的详细信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<RallyPostsDetailsVo> getRallyDetails(
            @Parameter(description = "约球帖子ID", required = true)
            @RequestParam Long postId,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long rallyApplicantId) {
        RallyPostsDetailsVo rallyDetails = rallyService.getRallyDetails(postId, rallyApplicantId);
        if (rallyDetails == null){
            return R.error(RallyResultEnum.RALLY_POST_NOT_EXIST);
        }
        return R.ok(rallyDetails);
    }

    /**
     * 创建约球
     * 
     * @param rallyPostsDto 约球数据
     * @param rallyApplicantId 申请人ID
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(summary = "创建约球", description = "创建新的约球活动")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R createRally(
            @Parameter(description = "约球活动数据", required = true)
            @RequestBody @Valid RallyPostsDto rallyPostsDto,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long rallyApplicantId) {
        RallyPosts rally = rallyService.createRally(rallyPostsDto, rallyApplicantId);
        if (rally == null){
            return R.error(SocialCode.RALLY_CREATE_FAILED);
        }else {
            return R.ok(rally);
        }
    }
    
    /**
     * 取消约球
     * 
     * @param postIdDto 帖子ID
     * @param cancellerId 取消者ID
     * @return 取消结果
     */
    @PostMapping("/cancelRally")
    @Operation(summary = "取消约球", description = "取消已创建的约球活动，仅创建者可操作")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R cancelRally(
            @Parameter(description = "约球帖子ID", required = true)
            @RequestBody PostIdDto postIdDto,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long cancellerId){
        if (cancellerId == null){
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        String string = rallyService.cancelRally(postIdDto.getPostId(), cancellerId);
        if (string == null){
            return R.error();
        }
        return R.ok(string);
    }
    
    /**
     * 加入约球
     * 
     * @param postIdDto 帖子ID
     * @param participantId 参与者ID
     * @return 加入结果
     */
    @PostMapping("/join")
    @Operation(summary = "加入约球", description = "申请加入约球活动，需要创建者审核")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "申请成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R joinRally(
            @Parameter(description = "约球帖子ID", required = true)
            @RequestBody PostIdDto postIdDto,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long participantId) {
        if (participantId == null){
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        String string = rallyService.joinRally(postIdDto.getPostId(), participantId);
        if (string == null){
            return R.error();
        }
        return R.ok(string);
    }

    /**
     * 取消加入约球
     * 
     * @param postIdDto 帖子ID
     * @param cancellerId 取消者ID
     * @return 取消加入结果
     */
    @PostMapping("/cancelJoin")
    @Operation(summary = "取消加入约球", description = "取消已提交的约球申请")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R cancelJoinRally(
            @Parameter(description = "约球帖子ID", required = true)
            @RequestBody PostIdDto postIdDto,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long cancellerId){
        if (cancellerId == null){
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        String rallyResult = rallyService.cancelJoinRally(postIdDto.getPostId(), cancellerId);
        if (rallyResult == null){
            return R.error();
        }
        return R.ok(rallyResult);
    }

    /**
     * 审核约球申请
     * 
     * @param inspectDto 审核数据
     * @param inspectorId 审核者ID
     * @return 审核结果
     */
    @PostMapping("/inspect")
    @Operation(summary = "审核约球申请", description = "审核用户的约球申请，仅约球创建者可操作")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "审核成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R inspectRally(
            @Parameter(description = "审核数据", required = true)
            @RequestBody InspectDto inspectDto,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long inspectorId){

        String rallyResult = rallyService.inspectRallyApply(inspectDto.getPostId(), inspectDto.getApplicantId(), inspectDto.getInspectResult(), inspectorId);
        if (rallyResult == null){
            return R.error();
        }
        return R.ok(rallyResult);

    }
    
    /**
     * 更新约球信息
     * 
     * @param updateRallyDto 更新数据
     * @param userId 用户ID
     * @return 更新结果
     */
    @PostMapping("/update")
    @Operation(summary = "更新约球信息", description = "更新约球活动信息，仅创建者可操作")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R updateRally(
            @Parameter(description = "更新数据", required = true)
            @RequestBody UpdateRallyDto updateRallyDto,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        if (userId == null){
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        String rallyResult = rallyService.updateRally(updateRallyDto, updateRallyDto.getId(), userId);
        if (rallyResult == null){
            return R.error();
        }
        return R.ok(rallyResult);
    }

    
    /**
     * 获取我的活动列表
     * 
     * @param type 类型
     * @param page 页码
     * @param pageSize 每页大小
     * @param userId 用户ID
     * @return 我的活动列表
     */
    @GetMapping("/myActivities")
    @Operation(summary = "获取我的活动列表", description = "获取当前用户相关的约球活动列表，支持按类型筛选（0-4）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<PaginationResult<RallyPostsVo>> myActivities(
            @Parameter(description = "活动类型（0-4）", required = true)
            @RequestParam Integer type,
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(HEADER_USER_ID) Long userId) {
        if (userId == null) {
            log.error("请求头中缺少{}", HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        // 验证type参数的有效性（现在只有0-4）
        if (type < 0 || type > 4) {
            throw new GloboxApplicationException("无效的查询类型");
        }

        PaginationResult<RallyPostsVo> rallyPostsVoPaginationResult = rallyService.myActivities(type, page, pageSize, userId);
        return R.ok(rallyPostsVoPaginationResult);
    }


    /**
     * 获取审核列表
     * 
     * @param postId 帖子ID
     * @param inspectorId 审核者ID
     * @return 审核列表
     */
    @GetMapping("/inspectList")
    @Operation(summary = "获取审核列表", description = "获取指定约球活动的申请审核列表，仅创建者可查看")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<PaginationResult<RallyApplicationVo>> inspectList(
            @Parameter(description = "约球帖子ID", required = true)
            @RequestParam Long postId,
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(HEADER_USER_ID) Long inspectorId){
        if (inspectorId == null){
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        PaginationResult<RallyApplicationVo> rallyApplications = rallyService.inspectList(postId,page,pageSize,inspectorId);
        return R.ok(rallyApplications);
    }
}

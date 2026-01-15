package com.unlimited.sports.globox.social.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;

import com.unlimited.sports.globox.model.social.dto.*;
import com.unlimited.sports.globox.model.social.entity.*;
import com.unlimited.sports.globox.model.social.vo.RallyApplicationVo;
import com.unlimited.sports.globox.model.social.vo.RallyPostsDetailsVo;
import com.unlimited.sports.globox.model.social.vo.RallyPostsVo;
import com.unlimited.sports.globox.model.social.vo.RallyQueryVo;
import com.unlimited.sports.globox.social.service.RallyService;
import com.unlimited.sports.globox.social.service.impl.RallyServiceImpl;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
public class RallyController {

    @Autowired
    private RallyService rallyService;

    /**
     * 获取约球列表
     * @return 约球列表分页结果
     */
    @GetMapping("/list")
    public R<RallyQueryVo> getRallyList(@Valid RallyQueryDto rallyQueryDto
            ) {
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
    public R<RallyPostsDetailsVo> getRallyDetails(@RequestParam Long postId,
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
    public R createRally(@RequestBody @Valid RallyPostsDto rallyPostsDto,
                         @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long rallyApplicantId) {
        if (rallyApplicantId == null){
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        RallyPosts rally = rallyService.createRally(rallyPostsDto, rallyApplicantId);
        if (rally == null){
            return R.error();
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
    public R cancelRally(@RequestBody PostIdDto postIdDto,
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
    public R joinRally(@RequestBody PostIdDto postIdDto,
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
    public R cancelJoinRally(@RequestBody PostIdDto postIdDto,
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
    public R inspectRally(@RequestBody InspectDto inspectDto,
                          @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long inspectorId){
        if (inspectorId == null){
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
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
    public R updateRally(@RequestBody UpdateRallyDto updateRallyDto,
                         @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId
    ) {
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
    public R<PaginationResult<RallyPostsVo>> myActivities(@RequestParam Integer type, @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "20") Integer pageSize,@RequestHeader(HEADER_USER_ID) Long userId) {
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
    public R<PaginationResult<RallyApplicationVo>> inspectList(@RequestParam Long postId,@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "20")
            Integer pageSize,
            @RequestHeader(HEADER_USER_ID) Long inspectorId){
        if (inspectorId == null){
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        PaginationResult<RallyApplicationVo> rallyApplications = rallyService.inspectList(postId,page,pageSize,inspectorId);
        return R.ok(rallyApplications);
    }
}

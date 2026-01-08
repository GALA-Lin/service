package com.unlimited.sports.globox.social.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.model.social.dto.RallyPostsDto;
import com.unlimited.sports.globox.model.social.dto.RallyQueryDto;

import com.unlimited.sports.globox.model.social.dto.UpdateRallyDto;
import com.unlimited.sports.globox.model.social.entity.RallyApplication;
import com.unlimited.sports.globox.model.social.entity.RallyPosts;
import com.unlimited.sports.globox.model.social.vo.RallyApplicationVo;
import com.unlimited.sports.globox.model.social.vo.RallyPostsDetailsVo;
import com.unlimited.sports.globox.model.social.vo.RallyPostsVo;
import com.unlimited.sports.globox.model.social.vo.RallyQueryVo;

import java.util.List;

/**
 * 约球服务
 */
public interface RallyService {
    /**
     * 获取约球列表
     *
     * @param rallyQueryDto
     * @return
     */
    PaginationResult<RallyPostsVo> getRallyPostsList(RallyQueryDto rallyQueryDto, Integer page, Integer pageSize);

    /**
     * 获取约球详情
     *
     * @param postId
     * @return
     */
    RallyPostsDetailsVo getRallyDetails(Long postId, Long rallyApplicantId);

    /**
     * 创建约球
     *
     * @param rallyPostsDto
     * @param rallyApplicantId
     * @return
     */
    RallyPosts createRally(RallyPostsDto rallyPostsDto, Long rallyApplicantId);

    /**
     * 加入约球
     *
     * @param postId
     * @param userId
     * @return
     */
    String joinRally(Long postId, Long userId);

    /**
     * 取消约球
     *
     * @param postId
     * @param userId
     * @return
     */
    String cancelRally(Long postId, Long userId);

    /**
     * 退出约球
     *
     * @param postId
     * @param userId
     * @return
     */
    String cancelJoinRally(Long postId, Long userId);

    /**
     * 审批约球申请
     *
     * @param postId
     * @param applicantId
     * @param inspectResult
     * @param inspectorId
     * @return
     */
    String inspectRallyApply(Long postId, Long applicantId, int inspectResult, Long inspectorId);

    /**
     * 修改约球
     *
     * @param updateRallyDto
     * @param rallyId
     * @param userId
     * @return
     */
    String updateRally(UpdateRallyDto updateRallyDto, Long rallyId, Long userId);

    /**
     * 我的活动
     *
     * @param type
     * @param page
     * @param pageSize
     * @param userId
     * @return
     */
    PaginationResult<RallyPostsVo> myActivities(Integer type, Integer page, Integer pageSize, Long userId);

    /**
     * 审批列表
     *
     * @param postId
     * @param inspectorId
     * @return
     */
    List<RallyApplicationVo> inspectList(Long postId, Long inspectorId);

    RallyQueryVo getRallyQueryList();

}

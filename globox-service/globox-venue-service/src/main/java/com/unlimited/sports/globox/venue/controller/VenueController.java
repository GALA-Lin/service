package com.unlimited.sports.globox.venue.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.venue.dto.*;
import com.unlimited.sports.globox.venue.service.IVenueSearchService;
import com.unlimited.sports.globox.venue.service.IVenueService;
import com.unlimited.sports.globox.model.venue.vo.VenueDetailVo;
import com.unlimited.sports.globox.model.venue.vo.VenueItemVo;
import com.unlimited.sports.globox.model.venue.vo.VenueReviewVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/venue/venues")
public class VenueController {

    @Autowired
    private IVenueSearchService venueSearchService;


    @Autowired
    private IVenueService venueService;

    /**
     * 获取场馆列表（支持搜索、过滤、排序）- 使用ES
     *
     * @param dto 查询条件（keyword, sortBy, minPrice, maxPrice等）
     * @return 分页的场馆列表
     */
    @GetMapping
    public R<PaginationResult<VenueItemVo>> getVenueList(@Valid GetVenueListDto dto) {
        PaginationResult<VenueItemVo> result = venueSearchService.searchVenues(dto);
        return R.ok(result);
    }


    /**
     * 获取场馆详情
     *
     * @param venueId 场馆ID
     * @return 场馆详情信息
     */
    @GetMapping("/{venueId}")
    public R<VenueDetailVo> getVenueDetail(@PathVariable Long venueId) {
        VenueDetailVo result = venueService.getVenueDetail(venueId);
        return R.ok(result);
    }

    /**
     * 获取场馆评论列表
     *
     * @param venueId 场馆ID
     * @param dto 查询条件（page, pageSize等）
     * @return 分页的评论列表
     */
    @GetMapping("/{venueId}/reviews")
    public R<PaginationResult<VenueReviewVo>> getVenueReviews(@PathVariable Long venueId, @Valid GetVenueReviewListDto dto) {
        dto.setVenueId(venueId);
        PaginationResult<VenueReviewVo> result = venueService.getVenueReviews(dto);
        return R.ok(result);
    }

    /**
     * 发布场馆评论
     *
     * @param venueId 场馆ID
     * @param dto 评论内容
     * @return 成功标识
     */
    @PostMapping("/{venueId}/reviews")
    public R<Void> postReview(@PathVariable Long venueId, @Valid @RequestBody PostVenueReviewDto dto,
                              HttpServletRequest request) {
        String userIdStr = request.getHeader(RequestHeaderConstants.HEADER_USER_ID);
        if(StringUtils.isBlank(userIdStr)) {
            throw new GloboxApplicationException("未获取到用户ID，无法发布评论");
        }
        long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new GloboxApplicationException("X-User-Id必须为数字");
        }
        dto.setUserId(userId);
        dto.setVenueId(venueId);
        venueService.postReview(dto);
        return R.ok(null);
    }
}

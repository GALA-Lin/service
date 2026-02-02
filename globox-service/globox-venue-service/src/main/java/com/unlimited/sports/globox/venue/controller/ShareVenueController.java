package com.unlimited.sports.globox.venue.controller;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.venue.dto.GetVenueReviewListDto;
import com.unlimited.sports.globox.model.venue.vo.VenueDetailVo;
import com.unlimited.sports.globox.model.venue.vo.VenueReviewVo;
import com.unlimited.sports.globox.venue.service.IVenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 场馆分享页接口（公开访问）
 */
@RestController
@RequestMapping("/share/venue/venues")
@Tag(name = "场馆分享", description = "场馆分享页公开接口")
public class ShareVenueController {

    @Autowired
    private IVenueService venueService;

    @GetMapping("/{venueId}")
    @Operation(summary = "获取场馆分享详情", description = "公开访问的场馆详情接口")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public R<VenueDetailVo> getVenueDetail(
            @Parameter(description = "场馆ID", required = true)
            @PathVariable Long venueId) {
        VenueDetailVo result = venueService.getVenueDetail(venueId);
        return R.ok(result);
    }

    @GetMapping("/{venueId}/reviews")
    @Operation(summary = "获取场馆分享评论列表", description = "公开访问的场馆评论列表接口")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public R<PaginationResult<VenueReviewVo>> getVenueReviews(
            @Parameter(description = "场馆ID", required = true)
            @PathVariable Long venueId,
            @Valid GetVenueReviewListDto dto) {
        dto.setVenueId(venueId);
        PaginationResult<VenueReviewVo> result = venueService.getVenueReviews(dto);
        return R.ok(result);
    }
}

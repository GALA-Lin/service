package com.unlimited.sports.globox.coach.controller;

import com.unlimited.sports.globox.coach.service.ICoachInfoService;
import com.unlimited.sports.globox.coach.service.ICoachReviewService;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.coach.dto.GetCoachReviewListDto;
import com.unlimited.sports.globox.model.coach.vo.CoachDetailVo;
import com.unlimited.sports.globox.model.coach.vo.CoachReviewVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 教练分享页接口（公开访问）
 */
@RestController
@RequestMapping("/share/coach")
@Tag(name = "教练分享", description = "教练分享页公开接口")
public class ShareCoachController {

    @Autowired
    private ICoachInfoService coachService;

    @Autowired
    private ICoachReviewService coachReviewService;

    @GetMapping("/coaches/{coachUserId}")
    @Operation(summary = "获取教练分享详情", description = "公开访问的教练详情接口")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public R<CoachDetailVo> getCoachDetail(
            @Parameter(description = "教练用户ID", required = true)
            @PathVariable Long coachUserId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        CoachDetailVo result = coachService.getCoachDetail(coachUserId, latitude, longitude);
        return R.ok(result);
    }

    @GetMapping("/reviews/coaches/{coachUserId}")
    @Operation(summary = "获取教练分享评论列表", description = "公开访问的教练评论列表接口")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public R<PaginationResult<CoachReviewVo>> getCoachReviews(
            @Parameter(description = "教练用户ID", required = true)
            @PathVariable Long coachUserId,
            @Valid GetCoachReviewListDto dto) {
        dto.setCoachUserId(coachUserId);
        PaginationResult<CoachReviewVo> result = coachReviewService.getCoachReviews(dto);
        return R.ok(result);
    }
}

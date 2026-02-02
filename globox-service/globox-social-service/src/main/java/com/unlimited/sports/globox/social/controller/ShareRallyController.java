package com.unlimited.sports.globox.social.controller;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.social.vo.RallyApplicationVo;
import com.unlimited.sports.globox.model.social.vo.RallyPostsDetailsVo;
import com.unlimited.sports.globox.social.mapper.RallyPostsMapper;
import com.unlimited.sports.globox.social.service.RallyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 约球分享页接口（公开访问）
 */
@RestController
@RequestMapping("/share/rally")
@Tag(name = "约球分享", description = "约球分享页公开接口")
public class ShareRallyController {

    @Autowired
    private RallyService rallyService;

    @Autowired
    private RallyPostsMapper rallyPostsMapper;

    @GetMapping("/details")
    @Operation(summary = "获取约球分享详情", description = "公开访问的约球详情接口")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<RallyPostsDetailsVo> getRallyDetails(
            @Parameter(description = "约球帖子ID", required = true)
            @RequestParam Long postId) {
        RallyPostsDetailsVo rallyDetails = rallyService.getRallyDetails(postId, null);
        if (rallyDetails == null) {
            return R.error();
        }
        return R.ok(rallyDetails);
    }

    @GetMapping("/inspectList")
    @Operation(summary = "获取约球审核列表（分享）", description = "公开访问的约球审核列表接口")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public R<PaginationResult<RallyApplicationVo>> inspectList(
            @Parameter(description = "约球帖子ID", required = true)
            @RequestParam Long postId,
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") Integer pageSize) {
        var rallyPosts = rallyPostsMapper.selectById(postId);
        Long initiatorId = rallyPosts == null ? null : rallyPosts.getInitiatorId();
        if (initiatorId == null) {
            return R.error();
        }
        PaginationResult<RallyApplicationVo> rallyApplications = rallyService.inspectList(postId, page, pageSize, initiatorId);
        return R.ok(rallyApplications);
    }
}

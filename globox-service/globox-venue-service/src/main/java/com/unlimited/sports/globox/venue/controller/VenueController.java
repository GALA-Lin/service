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
import com.unlimited.sports.globox.model.venue.vo.VenueDictVo;
import com.unlimited.sports.globox.model.venue.vo.VenueListResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/venue/venues")
public class VenueController {

    @Autowired
    private IVenueSearchService venueSearchService;


    @Autowired
    private IVenueService venueService;

    /**
     * 获取场馆列表（支持搜索、过滤、排序）
     * 返回场馆列表和所有可筛选的选项（场地类型、地面类型、场地片数、距离、设施）
     *
     * @param dto 查询条件（keyword, sortBy, minPrice, maxPrice等）
     * @return 场馆列表和筛选选项
     */
    @GetMapping
    public R<VenueListResponse> getVenueList(@Valid GetVenueListDto dto) {
        // 获取场馆搜索结果
        PaginationResult<VenueItemVo> venues = venueSearchService.searchVenues(dto);

        // 获取搜索过滤字典数据
        VenueDictVo dictVo = venueService.getSearchFilterDictionary();

        // 组装响应
        VenueListResponse result = VenueListResponse.builder()
                .venues(venues)
                .courtTypes(convertDictItems(dictVo.getCourtTypes()))
                .groundTypes(convertDictItems(dictVo.getGroundTypes()))
                .courtCountFilters(convertDictItems(dictVo.getCourtCountFilters()))
                .distances(convertDictItems(dictVo.getDistances()))
                .facilities(convertDictItems(dictVo.getFacilities()))
                .build();

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

    /**
     * 获取场馆搜索过滤字典数据
     *
     * @return 包含场地类型、地面类型、场地片数、距离、设施等字典数据
     */
    @GetMapping("/dict")
    public R<VenueDictVo> getSearchFilterDictionary() {
        VenueDictVo dictVo = venueService.getSearchFilterDictionary();
        return R.ok(dictVo);
    }

    /**
     * 转换字典项列表
     * 将VenueDictVo.DictItem列表转换为VenueListResponse.DictItem列表
     */
    private List<VenueListResponse.DictItem> convertDictItems(List<VenueDictVo.DictItem> dictItems) {
        if (dictItems == null) {
            return Collections.emptyList();
        }
        return dictItems.stream()
                .map(item -> VenueListResponse.DictItem.builder()
                        .value(item.getValue())
                        .description(item.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}

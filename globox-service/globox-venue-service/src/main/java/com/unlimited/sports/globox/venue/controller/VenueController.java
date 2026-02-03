package com.unlimited.sports.globox.venue.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.venue.dto.*;
import com.unlimited.sports.globox.model.venue.enums.ReviewDeleteOperatorType;
import com.unlimited.sports.globox.model.venue.vo.*;
import com.unlimited.sports.globox.venue.service.IVenueSearchService;
import com.unlimited.sports.globox.venue.service.IVenueService;
import com.unlimited.sports.globox.venue.service.impl.AwayVenueSearchService;
import com.unlimited.sports.globox.venue.service.impl.AwayVenueSearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/venue/venues")
public class VenueController {

    /**
     * 默认价格范围最小值
     */
    private static final BigDecimal DEFAULT_PRICE_MIN = BigDecimal.ZERO;

    /**
     * 默认价格范围最大值
     */
    private static final BigDecimal DEFAULT_PRICE_MAX = new BigDecimal("500");

    @Autowired
    private IVenueSearchService venueSearchService;

    @Autowired
    private IVenueService venueService;

    @Autowired
    private AwayVenueSearchService awayVenueSearchService;

    /**
     * 获取场馆列表（支持搜索、过滤、排序）
     * 返回场馆列表和所有可筛选的选项（场地类型、地面类型、场地片数、距离、设施）
     *
     * @param dto 查询条件（keyword, sortBy, minPrice, maxPrice等）
     * @return 场馆列表和筛选选项
     */
    @PostMapping
    public R<VenueListResponse> getVenueList(@Valid @RequestBody GetVenueListDto dto) {
        // 获取场馆搜索结果
        PaginationResult<VenueItemVo> venues = venueSearchService.searchVenues(dto);

        // 获取搜索过滤字典数据
        VenueDictVo dictVo = venueService.getSearchFilterDictionary();

        // 计算价格范围
        VenueListResponse.PriceRange priceRange = calculatePriceRange(venues.getList());

        // 组装响应
        VenueListResponse result = VenueListResponse.builder()
                .venues(venues)
                .courtTypes(convertDictItems(dictVo.getCourtTypes()))
                .groundTypes(convertDictItems(dictVo.getGroundTypes()))
                .courtCountFilters(convertDictItems(dictVo.getCourtCountFilters()))
                .distances(convertDictItems(dictVo.getDistances()))
                .facilities(convertDictItems(dictVo.getFacilities()))
                .priceRange(priceRange)
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
     * 获取场馆的最大可提前预约日期
     *
     * @param venueId 场馆ID
     * @return 最大可提前预约日期（天数）
     */
    @GetMapping("/{venueId}/max-advance-days")
    public R<Integer> getMaxAdvanceDays(@PathVariable Long venueId) {
        Integer maxAdvanceDays = venueService.getMaxAdvanceDays(venueId);
        return R.ok(maxAdvanceDays);
    }

    /**
     * 获取活动详情
     *
     * @param activityId 活动ID
     * @return 活动详情信息，包含基本信息和参与者列表
     */
    @GetMapping("/activities/{activityId}")
    public R<VenueActivityDetailVo> getActivityDetail(@PathVariable Long activityId) {
        VenueActivityDetailVo result = venueService.getActivityDetail(activityId);
        return R.ok(result);
    }

    /**
     * 根据场馆和日期查询活动列表
     *
     * @param dto 查询条件，包含场馆ID和活动日期
     * @return 活动列表，按时间排序
     */
    @GetMapping("/activities/list")
    public R<List<ActivityListVo>> getVenueActivityList(@Valid GetActivitiesByVenueDto dto) {
        log.info("获取活动list: {}",dto);
        List<ActivityListVo> result = venueService.getVenueActivityList(dto);
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
     * 删除场馆评论（用户只能删除自己的评论）
     *
     * @param reviewId 评论ID
     * @param userIdStr 用户ID
     * @return 成功标识
     */
    @DeleteMapping("/reviews/{reviewId}")
    public R<Void> deleteReview(@PathVariable Long reviewId,
                                @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) String userIdStr) {
        long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new GloboxApplicationException("获取用户ID失败");
        }

        DeleteVenueReviewDto dto = new DeleteVenueReviewDto();
        dto.setReviewId(reviewId);
        dto.setUserId(userId);
        dto.setDeleteOperatorType(ReviewDeleteOperatorType.USER_SELF.getValue());
        venueService.deleteReview(dto);
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
    private List<VenueDictItem> convertDictItems(List<VenueDictItem> dictItems) {
        if (dictItems == null) {
            return Collections.emptyList();
        }
        return dictItems;
    }

    /**
     * 计算价格范围
     * 从场馆列表中提取最小价格和最大价格供前端参考
     *
     * @param venues 场馆列表
     * @return 价格范围
     */
    private VenueListResponse.PriceRange calculatePriceRange(List<VenueItemVo> venues) {
        if (venues == null || venues.isEmpty()) {
            return VenueListResponse.PriceRange.builder()
                    .minPrice(DEFAULT_PRICE_MIN)
                    .maxPrice(DEFAULT_PRICE_MAX)
                    .build();
        }

        List<BigDecimal> prices = venues.stream()
                .map(VenueItemVo::getMinPrice)
                .toList();

        if (prices.isEmpty()) {
            return VenueListResponse.PriceRange.builder()
                    .minPrice(DEFAULT_PRICE_MIN)
                    .maxPrice(DEFAULT_PRICE_MAX)
                    .build();
        }

        BigDecimal minPrice = prices.stream()
                .min(BigDecimal::compareTo)
                .orElse(DEFAULT_PRICE_MIN);

        BigDecimal maxPrice = prices.stream()
                .max(BigDecimal::compareTo)
                .orElse(DEFAULT_PRICE_MAX);

        return VenueListResponse.PriceRange.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();
    }

    /**
     * 查询指定时间段内不可预订的Away球场ID
     * 用于测试 AwayVenueSearchService.getUnavailableAwayVenueIds 方法
     * todo 测试接口,后续删除,作为away球场不预定槽位的测试接口
     * @param dto 查询条件（date, startTime, endTime）
     * @return 不可预订的场馆ID集合
     */
    @PostMapping("/away/unavailable")
    public R<Map<String, Object>> queryUnavailableAwayVenues(@Valid @RequestBody QueryUnavailableAwayVenuesDto dto) {
        Set<Long> unavailableVenueIds = awayVenueSearchService.getUnavailableAwayVenueIds(
                dto.getDate(),
                dto.getStartTime(),
                dto.getEndTime()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("date", dto.getDate());
        response.put("startTime", dto.getStartTime());
        response.put("endTime", dto.getEndTime());
        response.put("unavailableVenueIds", unavailableVenueIds);
        response.put("count", unavailableVenueIds.size());

        return R.ok(response);
    }
}

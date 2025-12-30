package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.model.merchant.entity.VenueBookingSlot;
import com.unlimited.sports.globox.model.venue.dto.GetReviewRepliesDto;
import com.unlimited.sports.globox.model.venue.dto.GetVenueReviewListDto;
import com.unlimited.sports.globox.model.venue.dto.PostVenueReviewDto;
import com.unlimited.sports.globox.model.venue.vo.VenueDetailVo;
import com.unlimited.sports.globox.model.venue.vo.VenueReviewVo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 场馆服务接口
 * 提供场馆相关的业务逻辑处理
 */
public interface IVenueService {

    /**
     * 获取场馆详情
     *
     * @param venueId 场馆ID
     * @return 场馆详情信息，包含基本信息、评分、场地数量、设施和默认营业时间
     */
    VenueDetailVo getVenueDetail(Long venueId);

    /**
     * 获取场馆一级评论列表
     *
     * @param dto 查询条件，包含场馆ID和分页参数
     * @return 分页后的一级评论列表，包含用户信息和回复数量
     */
    PaginationResult<VenueReviewVo> getVenueReviews(GetVenueReviewListDto dto);


    /**
     * 发布场馆评论
     * 可以发布一级评论或回复
     *
     * @param dto 评论信息，包含场馆ID、用户ID、父评论ID（可选）、评分、内容、图片等
     */
    void postReview(PostVenueReviewDto dto);


}

package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.model.venue.dto.DeleteVenueReviewDto;
import com.unlimited.sports.globox.model.venue.dto.GetActivitiesByVenueDto;
import com.unlimited.sports.globox.model.venue.dto.GetVenueReviewListDto;
import com.unlimited.sports.globox.model.venue.dto.PostVenueReviewDto;
import com.unlimited.sports.globox.model.venue.vo.ActivityListVo;
import com.unlimited.sports.globox.model.venue.vo.VenueActivityDetailVo;
import com.unlimited.sports.globox.model.venue.vo.VenueDetailVo;
import com.unlimited.sports.globox.model.venue.vo.VenueDictVo;
import com.unlimited.sports.globox.model.venue.vo.VenueReviewVo;

import java.util.List;

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

    /**
     * 删除用户自己的评论
     *
     * @param dto 删除请求，包含评论ID、用户ID和删除操作人类型
     */
    void deleteReview(DeleteVenueReviewDto dto);

    /**
     * 获取场馆搜索过滤字典数据
     *
     * @return 包含场地类型、地面类型、场地片数、距离、设施等字典数据
     */
    VenueDictVo getSearchFilterDictionary();

    /**
     * 获取活动详情
     *
     * @param activityId 活动ID
     * @return 活动详情，包含基本信息和参与者列表
     */
    VenueActivityDetailVo getActivityDetail(Long activityId);

    /**
     * 根据场馆和日期查询活动列表
     * 只返回状态正常的活动，按时间排序
     *
     * @param dto 查询条件，包含场馆ID和活动日期
     * @return 活动列表，按startTime排序
     */
    List<ActivityListVo> getVenueActivityList(GetActivitiesByVenueDto dto);

    /**
     * 获取场馆的最大可提前预约日期
     *
     * @param venueId 场馆ID
     * @return 最大可提前预约日期（天数），如果场馆不存在或未配置则返回null
     */
    Integer getMaxAdvanceDays(Long venueId);

}

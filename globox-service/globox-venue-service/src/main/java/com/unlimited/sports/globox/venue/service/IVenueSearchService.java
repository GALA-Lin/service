package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.model.venue.dto.GetVenueListDto;
import com.unlimited.sports.globox.model.venue.vo.VenueItemVo;

/**
 * 场馆搜索服务接口 V2
 * 基于MySQL实现的场馆搜索功能，替代ES方案
 *
 * 主要改进：
 * - 使用MySQL替代Elasticsearch
 * - 支持距离筛选（公里数）
 * - 支持按场地数量排序
 * - 移除评分排序，改为场地数量排序
 */
public interface IVenueSearchService {

    /**
     * 搜索场馆列表（MySQL实现）
     *
     * 支持的搜索条件：
     * - 关键词搜索（场馆名称、地址、区域）
     * - 价格区间过滤
     * - 场地类型过滤（多选）
     * - 地面类型过滤（多选）
     * - 设施过滤（多选，基于venue_facility_relation表）
     * - 距离筛选（公里数）
     * - 时间段可用性过滤
     *
     * 支持的排序方式：
     * - distance：按距离排序（从近到远）
     * - price：按价格排序（从低到高）
     * - courtCount：按场地数量排序（从多到少）
     *
     * @param dto 搜索条件DTO，包含关键词、价格范围、场地类型、地面类型、设施、
     *            距离筛选、排序方式、经纬度、时间段、分页参数等
     * @return 分页后的场馆列表，包含场馆基本信息、最低价格、距离、场地数量等
     */
    PaginationResult<VenueItemVo> searchVenues(GetVenueListDto dto);
}

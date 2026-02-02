package com.unlimited.sports.globox.search.service;

import com.unlimited.sports.globox.model.venue.dto.GetVenueListDto;
import com.unlimited.sports.globox.model.venue.vo.VenueListResponse;
import com.unlimited.sports.globox.search.service.impl.VenueSearchServiceImpl;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.common.geo.GeoPoint;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场馆搜索服务接口
 * 处理场馆专用的搜索、过滤、排序逻辑
 *
 * 字段分层（根据UnifiedSearchDocument）：
 * 1. 公共字段 - 所有type都有
 * 2. 通用过滤字段 - 跨type的语义一致维度
 * 3. Type专属字段 - 各自特定的字段（以venue开头）
 */
public interface IVenueSearchService {

    /**
     * 搜索场馆（从DTO）
     *
     * @param dto 场馆搜索请求DTO
     * @return 场馆列表响应
     */
    VenueListResponse searchVenues(GetVenueListDto dto);

    /**
     * 构建公共查询条件
     * 应用到所有类型的数据（基于UnifiedSearchDocument的公共字段）
     * 包括：dataType、keyword(title/content)
     *
     * @param boolQuery bool查询对象
     * @param keyword 关键词
     * @return 构建后的boolQuery
     */
    BoolQueryBuilder buildCommonQuery(BoolQueryBuilder boolQuery, String keyword);

    /**
     * 构建通用过滤条件
     * 跨类型的通用字段过滤（基于UnifiedSearchDocument的通用过滤字段）
     * 包括：region、gender、ntrpMin、ntrpMax、priceMin、priceMax、location距离
     *
     * @param boolQuery bool查询对象
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param userLocation 用户位置
     * @param maxDistance 最大距离（公里）
     * @return 构建后的boolQuery
     */
    BoolQueryBuilder buildGeneralFilters(BoolQueryBuilder boolQuery, java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice, GeoPoint userLocation, Long maxDistance);

    /**
     * 构建场馆专属查询条件
     * 仅应用于VENUE类型的数据（基于UnifiedSearchDocument的场馆专属字段）
     * 包括：venueCourtTypes、venueGroundTypes、venueFacilities、venueCourtCount
     * 同时过滤掉不可用的场馆
     *
     * @param boolQuery bool查询对象
     * @param unavailableVenueIds 不可用的场馆ID列表
     * @return 构建后的boolQuery
     */
    BoolQueryBuilder buildVenueSpecificQuery(BoolQueryBuilder boolQuery,
                                             List<Integer> courtTypes,List<Integer> groundTypes,
                                             List<Integer> facilities,Integer courtCountFilter,
                                             List<Long> unavailableVenueIds);

    /**
     * 构建排序参数
     *
     * @param sortBy 排序字段：distance(距离) / price(价格) / courtcount(球场数量)
     * @param sortOrder 排序顺序：asc(升序) / desc(降序)
     * @param userLocation 用户位置
     * @return 排序结果对象
     */
    VenueSearchServiceImpl.SortResult buildSortBuilders(String sortBy, String sortOrder, GeoPoint userLocation);

    /**
     * 构建距离排序
     *
     * @param order 排序顺序
     * @param userLocation 用户位置
     * @return 排序列表
     */
    List<SortBuilder<?>> buildDistanceSorts(SortOrder order, GeoPoint userLocation);

    /**
     * 构建价格排序（包含距离辅助排序）
     *
     * @param order 排序顺序
     * @param userLocation 用户位置
     * @return 排序列表
     */
    List<SortBuilder<?>> buildPriceSorts(SortOrder order, GeoPoint userLocation);

    /**
     * 构建球场数量排序（包含距离辅助排序）
     *
     * @param order 排序顺序
     * @param userLocation 用户位置
     * @return 排序列表
     */
    List<SortBuilder<?>> buildCourtCountSorts(SortOrder order, GeoPoint userLocation);

    /**
     * 同步场馆数据到Elasticsearch
     *
     * @param updatedTime 上次同步时间，为null则全量同步
     * @return 同步的数据条数
     */
    int syncVenueData(LocalDateTime updatedTime);

    /**
     * 将VenueSearchDocument转换为VenueItemVo
     * @param document 场馆搜索文档
     * @param distance 距离（公里）
     * @return 场馆列表项视图对象
     */
    com.unlimited.sports.globox.model.venue.vo.VenueItemVo toListItemVo(com.unlimited.sports.globox.search.document.VenueSearchDocument document, java.math.BigDecimal distance);
}

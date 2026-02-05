package com.unlimited.sports.globox.search.service.impl;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.model.search.enums.SearchDocTypeEnum;
import com.unlimited.sports.globox.model.search.enums.SortOrderEnum;
import com.unlimited.sports.globox.model.venue.dto.GetVenueListDto;
import com.unlimited.sports.globox.model.venue.enums.CourtCountFilter;
import com.unlimited.sports.globox.model.venue.enums.CourtType;
import com.unlimited.sports.globox.model.venue.enums.DistanceFilter;
import com.unlimited.sports.globox.model.venue.enums.FacilityType;
import com.unlimited.sports.globox.model.venue.enums.GroundType;
import com.unlimited.sports.globox.model.venue.vo.VenueItemVo;
import com.unlimited.sports.globox.model.venue.vo.VenueListResponse;
import com.unlimited.sports.globox.search.constants.VenueSearchConstants;
import com.unlimited.sports.globox.dubbo.venue.IVenueSearchDataService;
import com.unlimited.sports.globox.search.document.VenueSearchDocument;
import com.unlimited.sports.globox.search.document.UnifiedSearchDocument;
import com.unlimited.sports.globox.model.venue.vo.VenueSyncVO;
import com.unlimited.sports.globox.search.service.IVenueSearchService;
import com.unlimited.sports.globox.search.service.IUnifiedSearchService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 场馆搜索服务实现
 * 处理场馆专用的搜索、过滤、排序逻辑
 */
@Slf4j
@Service
public class VenueSearchServiceImpl implements IVenueSearchService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @DubboReference(group = "rpc")
    private IVenueSearchDataService venueSearchDataService;


    @Lazy
    @Autowired
    private IUnifiedSearchService unifiedSearchService;

    @Value("${default_image.venue_list_cover:https://globox-dev-1386561970.cos.ap-chengdu.myqcloud.com/venue-review/venue-detail/2026-01-10/e743ec4475964ac7be237579c320f87b.jpg}")
    private String defaultVenueListCoverImage;
    /**
     * 搜索场馆（从DTO）
     *
     * @param dto 场馆搜索请求DTO
     * @return 场馆列表响应
     */
    @Override
    public VenueListResponse searchVenues(GetVenueListDto dto) {
        // 转换sortOrder格式（1=升序, 2=降序 -> asc/desc）
        String sortOrder = SortOrderEnum.fromValue(dto.getSortOrder()).getSortOrder();

        // 创建分页对象（GetVenueListDto的page从1开始，Pageable需要从0开始）
        Pageable pageable = PageRequest.of(
                dto.getPage() != null ? dto.getPage() - 1 : VenueSearchConstants.DEFAULT_PAGE_NUM,
                dto.getPageSize() != null ? dto.getPageSize() : VenueSearchConstants.DEFAULT_PAGE_SIZE
        );

        // 构建基础查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery = buildCommonQuery(boolQuery, dto.getKeyword());

        //构建通用过滤条件
        GeoPoint userLocation = new GeoPoint(dto.getLatitude(), dto.getLongitude());
        Double maxDistance = dto.getMaxDistance();
        boolQuery = buildGeneralFilters(boolQuery, dto.getMinPrice(), dto.getMaxPrice(), userLocation, maxDistance);

        // 获取不可用的场馆ID（如果提供了预订时间）
        List<Long> unavailableVenueIds = new ArrayList<>();
        if (dto.getBookingDate() != null && dto.getStartTime() != null && dto.getEndTime() != null) {
            // 校验时间范围
            if (dto.getStartTime().isAfter(dto.getEndTime()) || dto.getStartTime().equals(dto.getEndTime())) {
                log.warn("时间范围非法: startTime={}, endTime={}", dto.getStartTime(), dto.getEndTime());
            } else {
                RpcResult<List<Long>> result = venueSearchDataService.getUnavailableVenueIds(
                        dto.getBookingDate(),
                        dto.getStartTime(),
                        dto.getEndTime());
                unavailableVenueIds = (result == null || result.getData() == null) ? List.of() : result.getData();
            }
        }

        // 构建场馆专属查询条件
        boolQuery = buildVenueSpecificQuery(boolQuery, dto.getCourtTypes(),dto.getGroundTypes(),dto.getFacilities(),dto.getCourtCountFilter(), unavailableVenueIds);

        // 构建查询对象
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQuery);

        // 构建排序
        SortResult sortResult = buildSortBuilders(dto.getSortBy(), sortOrder, userLocation);
        sortResult.getSorts().forEach(queryBuilder::withSorts);

        // 设置分页
        queryBuilder.withPageable(pageable);

        // 执行查询
        NativeSearchQuery nativeSearchQuery = queryBuilder.build();
        assert nativeSearchQuery.getQuery() != null;
        log.info("完整ES查询JSON: {}", nativeSearchQuery.getQuery().toString());

        SearchHits<VenueSearchDocument> searchHits = elasticsearchOperations.search(
                nativeSearchQuery,
                VenueSearchDocument.class
        );

        log.info("场馆搜索: keyword={}, 命中数={}, 总数={}", dto.getKeyword(), searchHits.getSearchHits().size(), searchHits.getTotalHits());

        // 8. 转换搜索结果
        final int distanceSortIndex = sortResult.getDistanceSortIndex();
        List<VenueItemVo> venueList = searchHits.getSearchHits().stream()
                .map(hit -> {
                    try {
                        VenueSearchDocument doc = hit.getContent();
                        BigDecimal hitDistance = null;

                        // 从ES排序结果中提取距离值
                        if (hit.getSortValues().size() > distanceSortIndex) {
                            Object sortValue = hit.getSortValues().get(distanceSortIndex);
                            if (sortValue instanceof Double) {
                                hitDistance = BigDecimal.valueOf((Double) sortValue);
                            }
                        }

                        // 使用toListItemVo转换
                        return toListItemVo(doc, hitDistance);
                    } catch (Exception e) {
                        log.error("场馆搜索文档转换失败: docId={}", hit.getContent().getId(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        // 创建PaginationResult
        PaginationResult<VenueItemVo> paginationResult = PaginationResult.build(
                venueList,
                searchHits.getTotalHits(),
                dto.getPage() != null ? dto.getPage() : 1,
                dto.getPageSize() != null ? dto.getPageSize() : 10
        );

        // 组装响应
        return VenueListResponse.builder()
                .venues(paginationResult)
                .courtTypes(CourtType.getDictItems())
                .groundTypes(GroundType.getDictItems())
                .courtCountFilters(CourtCountFilter.getDictItems())
                .distances(DistanceFilter.getDictItems())
                .facilities(FacilityType.getDictItems())
                .priceRange(VenueListResponse.PriceRange.builder()
                        .minPrice(VenueSearchConstants.DEFAULT_PRICE_MIN)
                        .maxPrice(VenueSearchConstants.DEFAULT_PRICE_MAX)
                        .build())
                .build();
    }

    /**
     * 构建公共查询条件
     * 应用到所有类型的数据（基于UnifiedSearchDocument的公共字段）
     * 包括：dataType、keyword(title/content)
     * 查询策略（Multi-fields）：
     * - title 主字段：IK分词，精准匹配词语（如"超音速"），算分权重x2
     * - title.char 子字段：standard分词，单字符匹配（如"超"），算分权重x1
     * @param boolQuery bool查询对象
     * @param keyword 关键词
     * @return 构建后的boolQuery
     */
    @Override
    public BoolQueryBuilder buildCommonQuery(BoolQueryBuilder boolQuery, String keyword) {

        // 关键词搜索（同时搜索title和title.char字段）
        if (keyword != null && !keyword.isEmpty()) {
            boolQuery.must(
                QueryBuilders.multiMatchQuery(keyword)
                    .field("name", 2.0f)        // IK分词字段，权重x2
                    .field("name.char", 1.0f)   // 单字符字段，权重x1
                    .type(org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.BEST_FIELDS)
            );
        }

        return boolQuery;
    }

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
    @Override
    public BoolQueryBuilder buildGeneralFilters(BoolQueryBuilder boolQuery, BigDecimal minPrice,
                                                 BigDecimal maxPrice, GeoPoint userLocation, Double maxDistance) {
        // 价格范围过滤
        // 场馆价格范围[priceMin, priceMax]必须完全在用户价格范围[minPrice, maxPrice]内
        // 条件：场馆priceMin >= 用户minPrice AND 场馆priceMax <= 用户maxPrice
        if (minPrice != null || maxPrice != null) {
            if (minPrice != null) {
                // 场馆的最低价要 >= 用户的最低价
                boolQuery.filter(QueryBuilders.rangeQuery("priceMin").gte(minPrice.doubleValue()));
            }
            if (maxPrice != null) {
                // 场馆的最高价要 <= 用户的最高价
                boolQuery.filter(QueryBuilders.rangeQuery("priceMax").lte(maxPrice.doubleValue()));
            }
        }
        // 距离过滤
        if (maxDistance != null && maxDistance > 0) {
            boolQuery.filter(QueryBuilders.geoDistanceQuery("location")
                    .point(userLocation)
                    .distance(maxDistance, DistanceUnit.KILOMETERS));
        }

        return boolQuery;
    }

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
    @Override
    public BoolQueryBuilder buildVenueSpecificQuery(BoolQueryBuilder boolQuery,
                                                    List<Integer> courtTypes,List<Integer> groundTypes,
                                                    List<Integer> facilities,Integer courtCountFilter,
                                                    List<Long> unavailableVenueIds) {
        // 球场类型过滤
        if (courtTypes != null && !courtTypes.isEmpty()) {
            BoolQueryBuilder courtTypeQuery = QueryBuilders.boolQuery();
            courtTypes.forEach(type -> courtTypeQuery.should(
                    QueryBuilders.termQuery("courtTypes", type)
            ));
            boolQuery.filter(courtTypeQuery);
        }

        // 地面类型过滤
        if (groundTypes != null && !groundTypes.isEmpty()) {
            BoolQueryBuilder groundTypeQuery = QueryBuilders.boolQuery();
            groundTypes.forEach(type -> groundTypeQuery.should(
                    QueryBuilders.termQuery("groundTypes", type)
            ));
            boolQuery.filter(groundTypeQuery);
        }

        // 设施过滤
        if (facilities != null && !facilities.isEmpty()) {
            BoolQueryBuilder facilitiesQuery = QueryBuilders.boolQuery();
            facilities.forEach(facilityCode -> facilitiesQuery.should(
                    QueryBuilders.termQuery("facilities", facilityCode)
            ));
            boolQuery.filter(facilitiesQuery);
        }

        // 球场数量过滤
        if (courtCountFilter != null && courtCountFilter > 0) {
            CourtCountFilter filter = CourtCountFilter.fromValue(courtCountFilter);
            if (filter != null) {
                boolQuery.filter(QueryBuilders.rangeQuery("courtCount")
                        .gte(filter.getMinCount())
                        .lte(filter.getMaxCount()));
            }
        }

        // 过滤不可用的场馆
        if (unavailableVenueIds != null && !unavailableVenueIds.isEmpty()) {
            boolQuery.mustNot(QueryBuilders.termsQuery("venueId", unavailableVenueIds));
            log.info("过滤不可用场馆文档: {}", unavailableVenueIds);
        }

        return boolQuery;
    }

    /**
     * 构建排序参数
     *
     * @param sortBy 排序字段：distance(距离) / price(价格) / courtcount(球场数量)
     * @param sortOrder 排序顺序：asc(升序) / desc(降序)
     * @param userLocation 用户位置
     * @return 排序结果对象
     */
    @Override
    public SortResult buildSortBuilders(String sortBy, String sortOrder, GeoPoint userLocation) {
        List<SortBuilder<?>> sorts = new ArrayList<>();
        int distanceSortIndex = 0;

        // 按指定字段排序
        if (sortBy != null && !sortBy.isEmpty()) {
            SortOrder order = "desc".equalsIgnoreCase(sortOrder) ? SortOrder.DESC : SortOrder.ASC;

            distanceSortIndex = switch (sortBy.toLowerCase()) {
                case "distance" -> {
                    // 按距离排序
                    sorts.addAll(buildDistanceSorts(order, userLocation));
                    yield 0;
                }
                case "price" -> {
                    // 按价格排序 + 距离辅助排序
                    sorts.addAll(buildPriceSorts(order, userLocation));
                    yield 1;
                }
                case "courtcount" -> {
                    // 按球场数量排序 + 距离辅助排序
                    sorts.addAll(buildCourtCountSorts(order, userLocation));
                    yield 1;
                }
                default -> {
                    // 默认按距离排序
                    sorts.addAll(buildDistanceSorts(SortOrder.ASC, userLocation));
                    yield 0;
                }
            };
        } else {
            // 默认按距离升序排序
            sorts.addAll(buildDistanceSorts(SortOrder.ASC, userLocation));
        }

        // 添加最后的排序：按更新时间降序
        sorts.add(SortBuilders.fieldSort("updatedAt").order(SortOrder.DESC));

        return new SortResult(sorts, distanceSortIndex);
    }

    /**
     * 构建距离排序
     *
     * @param order 排序顺序
     * @param userLocation 用户位置
     * @return 排序列表
     */
    @Override
    public List<SortBuilder<?>> buildDistanceSorts(SortOrder order, GeoPoint userLocation) {
        List<SortBuilder<?>> sorts = new ArrayList<>();
        sorts.add(SortBuilders.geoDistanceSort("location", userLocation.lat(), userLocation.lon())
                .order(order)
                .unit(DistanceUnit.KILOMETERS));
        return sorts;
    }

    /**
     * 构建价格排序（包含距离辅助排序）
     *
     * @param order 排序顺序
     * @param userLocation 用户位置
     * @return 排序列表
     */
    @Override
    public List<SortBuilder<?>> buildPriceSorts(SortOrder order, GeoPoint userLocation) {
        List<SortBuilder<?>> sorts = new ArrayList<>();
        // 按最低价格排序
        sorts.add(SortBuilders.fieldSort("priceMin").order(order));
        // 添加距离排序以获取距离值
        sorts.add(SortBuilders.geoDistanceSort("location", userLocation.lat(), userLocation.lon())
                .order(SortOrder.ASC)
                .unit(DistanceUnit.KILOMETERS));
        return sorts;
    }

    /**
     * 构建球场数量排序（包含距离辅助排序）
     *
     * @param order 排序顺序
     * @param userLocation 用户位置
     * @return 排序列表
     */
    @Override
    public List<SortBuilder<?>> buildCourtCountSorts(SortOrder order, GeoPoint userLocation) {
        List<SortBuilder<?>> sorts = new ArrayList<>();
        // 按球场数量排序
        sorts.add(SortBuilders.fieldSort("courtCount").order(order));
        // 添加距离排序以获取距离值
        sorts.add(SortBuilders.geoDistanceSort("location", userLocation.lat(), userLocation.lon())
                .order(SortOrder.ASC)
                .unit(DistanceUnit.KILOMETERS));
        return sorts;
    }

    /**
     * 排序结果类
     * 用于返回排序列表和距离索引
     */
    @Getter
    @AllArgsConstructor
    public static class SortResult {
        private final List<SortBuilder<?>> sorts;
        private final int distanceSortIndex;
    }

    /**
     * 同步场馆数据到Elasticsearch
     *
     * 业务逻辑：
     * 1. 同步所有状态的场馆（正常、暂停等）
     * 2. 对于状态为NORMAL的场馆：保存或更新到ES和统一索引
     * 3. 对于状态不是NORMAL的场馆：从ES中删除
     *
     * @param updatedTime 上次同步时间，为null则全量同步
     * @return 同步的数据条数
     */
    @Override
    public int syncVenueData(LocalDateTime updatedTime) {
        try {
            log.info("开始同步场馆数据: updatedTime={}", updatedTime);

            // 调用RPC获取场馆数据（包含所有状态）
            RpcResult<List<VenueSyncVO>> result = venueSearchDataService.syncVenueData(updatedTime);
            if (result == null || !result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                log.info("没有需要同步的场馆数据");
                return 0;
            }

            List<VenueSyncVO> venueSyncVOs = result.getData();
            log.info("获取到场馆数据: 数量={}", venueSyncVOs.size());

            if (!venueSyncVOs.isEmpty()) {
                VenueSyncVO firstVo = venueSyncVOs.get(0);
                log.info("第一条数据: id={}, createdAt={}, updatedAt={}",
                    firstVo.getVenueId(), firstVo.getCreatedAt(), firstVo.getUpdatedAt());
            }

            // 将场馆按状态分类
            Map<Boolean, List<VenueSyncVO>> venusByStatus = venueSyncVOs.stream()
                    .collect(Collectors.partitioningBy(vo -> vo.getStatus() != null && vo.getStatus() == 1)); // 1=NORMAL

            List<VenueSyncVO> normalVenues = venusByStatus.get(true);  // 正常场馆
            List<VenueSyncVO> abnormalVenues = venusByStatus.get(false); // 非正常场馆

            int syncCount = 0;

            // 1. 处理正常场馆：转换并保存到ES
            if (normalVenues != null && !normalVenues.isEmpty()) {
                List<VenueSearchDocument> documents = normalVenues.stream()
                        .map(this::convertVenueSyncVOToDocument)
                        .filter(Objects::nonNull)
                        .toList();

                if (!documents.isEmpty()) {
                    elasticsearchOperations.save(documents);
                    log.info("正常场馆保存到ES: 成功条数={}", documents.size());

                    // 同步到统一索引
                    List<UnifiedSearchDocument> unifiedDocs = documents.stream()
                            .map(venue -> UnifiedSearchDocument.builder()
                                    .id(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.VENUE, venue.getVenueId()))
                                    .businessId(venue.getVenueId())
                                    .dataType(SearchDocTypeEnum.VENUE.getValue())
                                    .title(venue.getName())
                                    .content(venue.getDescription())
                                    .tags(venue.getCourtTypes() != null ?
                                            venue.getCourtTypes().stream().map(String::valueOf).collect(Collectors.toList()) : null)
                                    .location(venue.getLocation())
                                    .region(venue.getRegion())
                                    .coverUrl(venue.getCoverUrl())
                                    .score(venue.getRating() != null ? venue.getRating().doubleValue() : 0.0)
                                    .createdAt(venue.getCreatedAt())
                                    .updatedAt(venue.getUpdatedAt())
                                    .build())
                            .collect(Collectors.toList());
                    unifiedSearchService.saveOrUpdateToUnified(unifiedDocs);
                    syncCount = documents.size();
                }
            }

            // 2. 处理非正常场馆：从ES中删除
            if (abnormalVenues != null && !abnormalVenues.isEmpty()) {
                List<String> idsToDelete = abnormalVenues.stream()
                        .map(vo -> SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.VENUE, vo.getVenueId()))
                        .toList();
                IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder()
                        .addIds(idsToDelete.toArray(new String[0]));
                NativeSearchQuery deleteQuery = new NativeSearchQueryBuilder()
                        .withQuery(idsQueryBuilder)
                        .build();
                elasticsearchOperations.delete(deleteQuery, VenueSearchDocument.class);
                unifiedSearchService.deleteFromUnified(idsToDelete);
                log.info("非正常场馆从ES删除: 删除条数={}", idsToDelete.size());

            }

            log.info("场馆数据同步完成: 正常场馆数={}, 非正常场馆数={}",
                    normalVenues != null ? normalVenues.size() : 0,
                    abnormalVenues != null ? abnormalVenues.size() : 0);

            return syncCount;

        } catch (Exception e) {
            log.error("同步场馆数据异常: updatedTime={}", updatedTime, e);
            return 0;
        }
    }

    /**
     * 将VenueSyncVO转换为VenueSearchDocument
     *
     * @param vo 场馆同步VO
     * @return VenueSearchDocument文档
     */
    private VenueSearchDocument convertVenueSyncVOToDocument(VenueSyncVO vo) {
        try {
            if (vo == null || vo.getVenueId() == null) {
                return null;
            }

            Point location = null;
            if (vo.getLatitude() != null && vo.getLongitude() != null) {
                location = new Point(
                        vo.getLongitude().doubleValue(),
                        vo.getLatitude().doubleValue()
                );
            }

            log.info("原价格 min{} max{}",vo.getPriceMax(),vo.getPriceMin());
            return VenueSearchDocument.builder()
                    .id(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.VENUE,vo.getVenueId()))
                    .venueId(vo.getVenueId())
                    .name(vo.getVenueName())
                    .description(vo.getVenueDescription())
                    .region(vo.getRegion())
                    // 价格维度转换：数据库存储的是30分钟的价格，ES中存储为1小时的价格（乘以2）
                    .priceMin(vo.getPriceMin() != null ? vo.getPriceMin().multiply(new BigDecimal("2")).doubleValue() : null)
                    .priceMax(vo.getPriceMax() != null ? vo.getPriceMax().multiply(new BigDecimal("2")).doubleValue() : null)
                    .rating(vo.getRating() != null ? vo.getRating().doubleValue() : null)
                    .ratingCount(vo.getRatingCount())
                    .coverUrl(vo.getCoverUrl())
                    .status(vo.getStatus())
                    .createdAt(vo.getCreatedAt())
                    .updatedAt(vo.getUpdatedAt())
                    .location(location)
                    .venueType(vo.getVenueType())
                    .courtCount(vo.getCourtCount())
                    .courtTypes(vo.getCourtTypes())
                    .groundTypes(vo.getGroundTypes())
                    .facilities(vo.getFacilities())
                    .build();

        } catch (Exception e) {
            log.error("转换VenueSyncVO为Document失败: venueId={}", vo.getVenueId(), e);
            return null;
        }
    }

    @Override
    public VenueItemVo toListItemVo(VenueSearchDocument document, BigDecimal distance) {
        if (document == null) {
            return null;
        }

        // 从Point中提取经纬度
        Double lat = null;
        Double lon = null;
        if (document.getLocation() != null) {
            lat = document.getLocation().getY();  // 纬度
            lon = document.getLocation().getX();  // 经度
        }

        // 转换球场类型描述
        List<String> courtTypesDesc = CourtType.getDescriptionsByValues(document.getCourtTypes());

        // 转换地面类型描述
        List<String> groundTypesDesc = GroundType.getDescriptionsByValues(document.getGroundTypes());

        // 转换设施代码为设施描述
        List<String> facilitiesDesc = FacilityType.getDescriptionsByValues(document.getFacilities());

        // 距离保留2位小数
        BigDecimal formattedDistance = distance;
        if (distance != null) {
            formattedDistance = distance.setScale(2, RoundingMode.HALF_UP);
        }

        // 构建VenueItemVo
        return VenueItemVo.builder()
                .venueId(document.getVenueId())
                .name(document.getName())
                .region(document.getRegion())
                .distance(formattedDistance)
                .coverImage(document.getCoverUrl() != null ? document.getCoverUrl() : defaultVenueListCoverImage)
                .avgRating(document.getRating() != null ? BigDecimal.valueOf(document.getRating()) : BigDecimal.ZERO)
                .ratingCount(document.getRatingCount() != null ? document.getRatingCount() : 0)
                .minPrice(document.getPriceMin() != null ? BigDecimal.valueOf(document.getPriceMin()) : BigDecimal.ZERO)
                .courtTypes(document.getCourtTypes() != null ? document.getCourtTypes() : new ArrayList<>())
                .courtTypesDesc(courtTypesDesc)
                .groundTypes(document.getGroundTypes() != null ? document.getGroundTypes() : new ArrayList<>())
                .groundTypesDesc(groundTypesDesc)
                .facilities(facilitiesDesc)
                .courtCount(document.getCourtCount() != null ? document.getCourtCount() : 0)
                .latitude(lat != null ? BigDecimal.valueOf(lat) : BigDecimal.ZERO)
                .longitude(lon != null ? BigDecimal.valueOf(lon) : BigDecimal.ZERO)
                .build();
    }
}

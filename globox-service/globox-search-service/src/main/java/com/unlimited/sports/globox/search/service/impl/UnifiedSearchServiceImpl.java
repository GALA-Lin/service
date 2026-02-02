package com.unlimited.sports.globox.search.service.impl;

import com.alibaba.fastjson2.JSON;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.model.search.dto.SearchResultItemVo;
import com.unlimited.sports.globox.model.search.dto.UnifiedSearchResultVo;
import com.unlimited.sports.globox.model.search.enums.SearchDocTypeEnum;
import com.unlimited.sports.globox.dubbo.social.ISocialFollowDataService;
import com.unlimited.sports.globox.model.auth.vo.UserListItemVo;
import com.unlimited.sports.globox.model.social.dto.NoteStatisticsDto;
import com.unlimited.sports.globox.search.document.NoteSearchDocument;
import com.unlimited.sports.globox.search.document.UnifiedSearchDocument;
import com.unlimited.sports.globox.search.document.UserSearchDocument;
import com.unlimited.sports.globox.search.document.VenueSearchDocument;
import com.unlimited.sports.globox.search.service.IUnifiedSearchService;
import com.unlimited.sports.globox.search.service.IUserSearchService;
import com.unlimited.sports.globox.search.service.INoteSearchService;
import com.unlimited.sports.globox.search.service.IVenueSearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 统一搜索服务实现
 * 用于全局搜索（如搜"网球"返回场馆+教练+笔记+约球混合结果）
 */
@Slf4j
@Service
public class UnifiedSearchServiceImpl implements IUnifiedSearchService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private IUserSearchService userSearchService;

    @Autowired
    private IVenueSearchService venueSearchService;

    @Autowired
    private INoteSearchService noteSearchService;

    @DubboReference(group = "rpc")
    private ISocialFollowDataService socialFollowDataService;

    private static final String INDEX_VENUE = "globox-venue";
    private static final String INDEX_NOTE = "globox-note";
    private static final String INDEX_USER = "globox-user";

    /**
     * 统一搜索
     * 第一阶段：从统一索引查询分页数据（支持热度/时间/相关性排序）
     * 第二阶段：根据类型批量获取各独立索引的完整详情
     *
     * @param keyword  搜索关键词
     * @param types    类型过滤（可选，如 ["VENUE", "NOTE"]）
     * @param sortBy   排序方式：relevance(相关性,默认) | score(热度) | time(时间)
     * @param page     页码（从1开始）
     * @param pageSize 每页大小
     * @return 混合搜索结果
     */
    public UnifiedSearchResultVo searchUnified(String keyword, List<String> types,
                                                 String sortBy, Integer page, Integer pageSize) {
        log.info("统一搜索: keyword={}, types={}, sortBy={}, page={}, pageSize={}",
                keyword, types, sortBy, page, pageSize);
        // 参数默认值
        page = (page == null || page <= 0) ? 1 : page;
        pageSize = (pageSize == null || pageSize <= 0) ? 20 : pageSize;
        sortBy = (sortBy == null || sortBy.isEmpty()) ? "relevance" : sortBy;
        try {
            // 统一索引分页查询
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            // 关键词搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                boolQuery.must(QueryBuilders.multiMatchQuery(keyword)
                        .field("title", 2.0f)
                        .field("content", 1.0f)
                        .field("tags", 1.5f)
                        .type(MultiMatchQueryBuilder.Type.BEST_FIELDS));
            }
            // 类型过滤
            if (types != null && !types.isEmpty()) {
                boolQuery.filter(QueryBuilders.termsQuery("dataType", types));
            }
            // 构建排序
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                    .withQuery(boolQuery)
                    .withPageable(PageRequest.of(page - 1, pageSize));
            // 排序策略
            switch (sortBy) {
                case "score":
                    queryBuilder.withSorts(SortBuilders.fieldSort("score").order(SortOrder.DESC));
                    queryBuilder.withSorts(SortBuilders.scoreSort());
                    break;
                case "time":
                    queryBuilder.withSorts(SortBuilders.fieldSort("createdAt").order(SortOrder.DESC));
                    break;
                default: // relevance
                    queryBuilder.withSorts(SortBuilders.scoreSort());
                    queryBuilder.withSorts(SortBuilders.fieldSort("score").order(SortOrder.DESC));
            }

            NativeSearchQuery nativeSearchQuery = queryBuilder.build();

            // 执行第一阶段查询
            SearchHits<UnifiedSearchDocument> searchHits = elasticsearchOperations.search(
                    nativeSearchQuery, UnifiedSearchDocument.class);

            long total = searchHits.getTotalHits();
            log.info("第一阶段查询完成: 命中数={}, 总数={}", searchHits.getSearchHits().size(), total);

            if (searchHits.getSearchHits().isEmpty()) {
                return UnifiedSearchResultVo.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .total(0L)
                        .totalPages(0)
                        .items(Collections.emptyList())
                        .build();
            }

            // 提取ID和类型，保留顺序
            List<SearchHit<UnifiedSearchDocument>> hits = searchHits.getSearchHits();
            Map<String, List<Long>> typeToIds = new LinkedHashMap<>();
            List<String> orderedKeys = new ArrayList<>();

            hits.forEach(hit -> {
                UnifiedSearchDocument doc = hit.getContent();
                String type = doc.getDataType();
                Long bizId = doc.getBusinessId();

                typeToIds.computeIfAbsent(type, k -> new ArrayList<>()).add(bizId);
                orderedKeys.add(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.valueOf(type), bizId));
            });

            // 原生 mget 跨索引批量获取（单次请求）
            Map<String, Object> detailMap = fetchDetailsByMget(typeToIds);
            log.info("第二阶段查询完成: 获取详情数={}", detailMap.size());

            List<SearchResultItemVo> resultItems = new ArrayList<>();

            IntStream.range(0, hits.size())
                    .mapToObj(i -> {
                        SearchHit<UnifiedSearchDocument> hit = hits.get(i);
                        UnifiedSearchDocument unifiedDoc = hit.getContent();
                        String key = orderedKeys.get(i);
                        Object data = detailMap.get(key);

                        return SearchResultItemVo.builder()
                                .type(unifiedDoc.getDataType())
                                .businessId(unifiedDoc.getBusinessId())
                                .data(data != null ? data : unifiedDoc)
                                .relevanceScore(hit.getScore())
                                .build();
                    })
                    .forEach(resultItems::add);

            int totalPages = (int) Math.ceil((double) total / pageSize);

            return UnifiedSearchResultVo.builder()
                    .page(page)
                    .pageSize(pageSize)
                    .total(total)
                    .totalPages(totalPages)
                    .items(resultItems)
                    .build();

        } catch (Exception e) {
            log.error("统一搜索异常: keyword={}", keyword, e);
            return UnifiedSearchResultVo.builder()
                    .page(page)
                    .pageSize(pageSize)
                    .total(0L)
                    .totalPages(0)
                    .items(Collections.emptyList())
                    .build();
        }
    }

    /**
     * 使用原生 mget 跨索引批量获取详情并转换为ListItemVo（单次请求）
     */
    private Map<String, Object> fetchDetailsByMget(Map<String, List<Long>> typeToIds) {
        Map<String, Object> detailMap = new HashMap<>();

        try {
            MultiGetRequest request = new MultiGetRequest();
            // 添加所有需要获取的文档
            List<Long> venueIds = typeToIds.get(SearchDocTypeEnum.VENUE.getValue());
            if (venueIds != null) {
                venueIds.forEach(id -> request.add(new MultiGetRequest.Item(INDEX_VENUE, SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.VENUE, id))));
            }
            List<Long> noteIds = typeToIds.get(SearchDocTypeEnum.NOTE.getValue());
            if (noteIds != null) {
                noteIds.forEach(id -> request.add(new MultiGetRequest.Item(INDEX_NOTE, SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.NOTE, id))));
            }
            List<Long> userIds = typeToIds.get(SearchDocTypeEnum.USER.getValue());
            if (userIds != null) {
                userIds.forEach(id -> request.add(new MultiGetRequest.Item(INDEX_USER, SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.USER, id))));
            }
            if (request.getItems().isEmpty()) {
                return detailMap;
            }

            // 单次 mget 请求
            MultiGetResponse response = restHighLevelClient.mget(request, RequestOptions.DEFAULT);

            // 收集数据和需要用户信息的文档
            Map<String, Object> pendingUserDocuments = new HashMap<>();  // 存储需要用户信息的文档 key->文档对象
            Set<Long> requiredUserIds = new HashSet<>();  // 收集所有需要的userId
            List<UserListItemVo> userVosNeedingFollowers = new ArrayList<>();  // 收集需要获取粉丝数的用户VO

            Arrays.stream(response.getResponses())
                    .filter(item -> item != null && !item.isFailed() && item.getResponse() != null && item.getResponse().isExists())
                    .forEach(item -> {
                        String index = item.getIndex();
                        String sourceJson = item.getResponse().getSourceAsString();
                        switch (index) {
                            case INDEX_VENUE -> {
                                VenueSearchDocument venue = JSON.parseObject(sourceJson, VenueSearchDocument.class);
                                if (venue != null && venue.getVenueId() != null) {
                                    Object venueItemVo = venueSearchService.toListItemVo(venue, null);
                                    detailMap.put(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.VENUE, venue.getVenueId()), venueItemVo);
                                }
                            }
                            case INDEX_NOTE -> {
                                NoteSearchDocument note = JSON.parseObject(sourceJson, NoteSearchDocument.class);
                                if (note != null && note.getNoteId() != null) {
                                    // 笔记需要用户信息，加入待处理
                                    pendingUserDocuments.put(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.NOTE, note.getNoteId()), note);
                                    requiredUserIds.add(note.getUserId());
                                }
                            }
                            case INDEX_USER -> {
                                UserSearchDocument user = JSON.parseObject(sourceJson, UserSearchDocument.class);
                                if (user != null && user.getUserId() != null) {
                                    UserListItemVo userItemVo = userSearchService.toListItemVo(user);
                                    detailMap.put(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.USER, user.getUserId()), userItemVo);
                                    // 记录需要获取粉丝数的用户VO
                                    if (userItemVo != null) {
                                        userVosNeedingFollowers.add(userItemVo);
                                    }
                                }
                            }
                        }
                    });

            // 批量获取所有需要的用户信息
            Map<Long, UserSearchDocument> userMap = new HashMap<>();
            if (!requiredUserIds.isEmpty()) {
                userMap.putAll(userSearchService.getUsersByIds(new ArrayList<>(requiredUserIds)));
            }
            // 处理需要用户信息的文档
            pendingUserDocuments.forEach((key, doc) -> {
                if (doc instanceof NoteSearchDocument note) {
                    UserSearchDocument userDoc = userMap.get(note.getUserId());
                    Object noteItemVo = noteSearchService
                            .toListItemVo(note, userDoc, NoteStatisticsDto.builder()
                                    .noteId(note.getNoteId())
                                    .likeCount(note.getLikes() != null ? note.getLikes() : 0)
                                    .commentCount(note.getComments() != null ? note.getComments() : 0)
                                    .isLiked(false)
                                    .build());
                    detailMap.put(key, noteItemVo);
                }
                // 后续扩展：如果有其他类型也需要用户信息，可以在这里添加
            });

            // 批量获取所有用户的粉丝数量
            if (!userVosNeedingFollowers.isEmpty()) {
                enrichUserVosWithFollowers(userVosNeedingFollowers);
            }

        } catch (Exception e) {
            log.error("mget 跨索引查询异常", e);
        }
        return detailMap;
    }

    /**
     * 批量获取粉丝数量并填充到UserListItemVo
     */
    private void enrichUserVosWithFollowers(List<UserListItemVo> userVos) {
        try {
            List<Long> userIds = userVos.stream()
                    .map(UserListItemVo::getUserId)
                    .toList();

            if (userIds.isEmpty()) {
                return;
            }

            RpcResult<Map<Long, Integer>> result = socialFollowDataService.getUserFollowerCounts(userIds);
            Assert.rpcResultOk(result);
            if (result.getData() != null) {
                Map<Long, Integer> followerCounts = result.getData();
                userVos.forEach(userVo -> {
                    Integer followerCount = followerCounts.getOrDefault(userVo.getUserId(), 0);
                    userVo.setFollowers(followerCount);
                });
                log.debug("批量获取粉丝数量完成: 用户数={}", userVos.size());
            } else {
                log.warn("批量获取粉丝数量失败, 使用默认值0");
                userVos.forEach(userVo -> userVo.setFollowers(0));
            }
        } catch (Exception e) {
            log.error("批量获取粉丝数量异常", e);
            userVos.forEach(userVo -> userVo.setFollowers(0));
        }
    }

    @Override
    public void saveOrUpdateToUnified(List<UnifiedSearchDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        try {
            elasticsearchOperations.save(documents);
            log.info("保存或更新统一索引数据: 数量={}", documents.size());
        } catch (Exception e) {
            log.error("保存或更新统一索引数据异常", e);
        }
    }

    @Override
    public void deleteFromUnified(List<String> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return;
        }
        try {
            // 批量删除（基于查询的批量删除）
            IdsQueryBuilder idsQuery = new IdsQueryBuilder().addIds(docIds.toArray(new String[0]));
            NativeSearchQuery deleteQuery = new NativeSearchQueryBuilder()
                    .withQuery(idsQuery)
                    .build();
            elasticsearchOperations.delete(deleteQuery, UnifiedSearchDocument.class);
            log.info("删除统一索引数据: 数量={}", docIds.size());
        } catch (Exception e) {
            log.error("删除统一索引数据异常", e);
        }
    }

}

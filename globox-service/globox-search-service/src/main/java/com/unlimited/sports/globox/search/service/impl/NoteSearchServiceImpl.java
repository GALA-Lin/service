package com.unlimited.sports.globox.search.service.impl;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.vo.SearchDocumentDto;
import com.unlimited.sports.globox.common.vo.SearchResultItem;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.search.constants.NoteSearchConstants;
import com.unlimited.sports.globox.model.search.enums.NoteSortTypeEnum;
import com.unlimited.sports.globox.model.search.enums.SearchDocTypeEnum;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.vo.NoteSyncVo;
import com.unlimited.sports.globox.model.social.vo.NoteItemVo;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.dubbo.social.INoteSearchDataService;
import com.unlimited.sports.globox.search.document.UnifiedSearchDocument;
import com.unlimited.sports.globox.search.service.INoteSearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 笔记搜索服务实现
 * 处理笔记搜索、过滤、排序、数据同步等业务逻辑
 */
@Slf4j
@Service
public class NoteSearchServiceImpl implements INoteSearchService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @DubboReference(group = "rpc")
    private INoteSearchDataService noteSearchDataService;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;

    /**
     * 搜索笔记 - 支持关键词、标签、排序
     *
     * @param keyword 搜索关键词 (会匹配 title 和 content)
     * @param sortBy 排序方式: latest / hottest / selected
     * @param page 分页页码 (从1开始)
     * @param pageSize 每页大小
     */
    @Override
    public PaginationResult<NoteItemVo> searchNotes(String keyword, String tag, String sortBy, Integer page, Integer pageSize) {
        try {
            log.info("开始搜索笔记: keyword={}, tag={}, sortBy={}, page={}, pageSize={}",
                    keyword, tag, sortBy, page, pageSize);

            // 1. 参数验证和默认值设置
            page = (page == null || page <= 0) ? 1 : page;
            pageSize = (pageSize == null || pageSize <= 0) ? 10 : pageSize;
            NoteSortTypeEnum sortType = NoteSortTypeEnum.fromCode(sortBy);

            // 2. 构建查询条件
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery.filter(QueryBuilders.termQuery("dataType", SearchDocTypeEnum.NOTE.getValue()));

            boolQuery = buildCommonQuery(boolQuery, keyword);
            boolQuery = buildNoteSpecificQuery(boolQuery, tag);

            // 3. 构建排序
            List<SortBuilder<?>> sorts = buildSortBuilders(sortType.getCode());

            // 4. 构建分页对象（ES中page从0开始）
            Pageable pageable = PageRequest.of(page - 1, pageSize);

            // 5. 构建查询对象
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                    .withQuery(boolQuery);

            sorts.forEach(queryBuilder::withSorts);
            queryBuilder.withPageable(pageable);

            // 6. 执行查询
            NativeSearchQuery nativeSearchQuery = queryBuilder.build();
            SearchHits<UnifiedSearchDocument> searchHits = elasticsearchOperations.search(
                    nativeSearchQuery,
                    UnifiedSearchDocument.class
            );

            log.info("笔记搜索结果: 命中数={}, 总数={}", searchHits.getSearchHits().size(), searchHits.getTotalHits());

            // 7. 提取查询结果
            List<UnifiedSearchDocument> docs = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .toList();

            // 8. 提取所有userId，批量获取用户信息
            Set<Long> userIds = docs.stream()
                    .map(doc -> Long.parseLong(doc.getCreatorId()))
                    .collect(Collectors.toSet());

            BatchUserInfoRequest request = new BatchUserInfoRequest();
            request.setUserIds(new ArrayList<>(userIds));
            RpcResult<BatchUserInfoResponse> result = userDubboService.batchGetUserInfo(request);
            Assert.rpcResultOk(result);
            Map<Long, UserInfoVo> userInfoMap;
            if(result.getData() != null && !CollectionUtils.isEmpty(result.getData().getUsers())) {
                userInfoMap = result.getData().getUsers().stream().collect(Collectors.toMap(UserInfoVo::getUserId,userInfoVo ->  userInfoVo));
            }else {
                userInfoMap = new HashMap<>();
            }
            // 9. 转换为NoteItemVo并填充用户信息
            List<SearchResultItem<NoteItemVo>> resultItems = docs.stream()
                    .map(doc -> {
                        try {
                            Long userId = Long.parseLong(doc.getCreatorId());
                            UserInfoVo userInfo = userInfoMap.getOrDefault(userId, null);

                            // 创建SearchDocumentDto
                            SearchDocumentDto searchDocDto = SearchDocumentDto.builder()
                                    .businessId(doc.getBusinessId())
                                    .dataType(doc.getDataType())
                                    .creatorId(doc.getCreatorId())
                                    .title(doc.getTitle())
                                    .content(doc.getContent())
                                    .coverUrl(doc.getCoverUrl())
                                    .likes(doc.getLikes())
                                    .comments(doc.getComments())
                                    .saves(doc.getSaves())
                                    .noteMediaType(doc.getNoteMediaType())
                                    .noteAllowComment(doc.getNoteAllowComment())
                                    .status(doc.getStatus() != null ? String.valueOf(doc.getStatus()) : null)
                                    .createdAt(doc.getCreatedAt())
                                    .build();

                            // 使用NoteItemVo的转换方法
                            return NoteItemVo.fromSearchDocument(searchDocDto, userInfo);
                        } catch (Exception e) {
                            log.error("笔记转换失败: noteId={}", doc.getBusinessId(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            // 转换搜索结果为NoteItemVo列表
            List<NoteItemVo> noteItems = resultItems.stream()
                    .filter(item -> item.getData() != null)
                    .map(SearchResultItem::getData)
                    .collect(Collectors.toList());

            // 10. 返回分页结果
            return PaginationResult.build(noteItems, searchHits.getTotalHits(), page, pageSize);

        } catch (Exception e) {
            log.error("搜索笔记异常", e);
            return PaginationResult.build(Collections.emptyList(), 0, page == null ? 1 : page, pageSize == null ? 10 : pageSize);
        }
    }

    /**
     * 构建公共查询条件
     */
    @Override
    public BoolQueryBuilder buildCommonQuery(BoolQueryBuilder boolQuery, String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            boolQuery.must(
                QueryBuilders.multiMatchQuery(keyword)
                    .field("title", NoteSearchConstants.TITLE_SEARCH_WEIGHT)
                    .field("content", NoteSearchConstants.CONTENT_SEARCH_WEIGHT)
                    .type(org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.BEST_FIELDS)
            );
        }
        return boolQuery;
    }

    /**
     * 构建笔记专属过滤条件
     */
    @Override
    public BoolQueryBuilder buildNoteSpecificQuery(BoolQueryBuilder boolQuery, String tag) {
        // 标签过滤：单个标签
        if (tag != null && !tag.isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("tags", tag));
        }
        return boolQuery;
    }

    /**
     * 构建排序参数
     */
    @Override
    public List<SortBuilder<?>> buildSortBuilders(String sortBy) {
        NoteSortTypeEnum sortType = NoteSortTypeEnum.fromCode(sortBy);
        return switch (sortType) {
            case HOTTEST -> buildHottestSorts();
            case SELECTED -> buildSelectedSorts();
            default -> buildLatestSorts();
        };
    }

    /**
     * 构建最新排序
     */
    @Override
    public List<SortBuilder<?>> buildLatestSorts() {
        List<SortBuilder<?>> sorts = new ArrayList<>();
        sorts.add(SortBuilders.fieldSort("createdAt").order(SortOrder.DESC));
        sorts.add(SortBuilders.fieldSort("businessId").order(SortOrder.DESC));
        return sorts;
    }

    /**
     * 构建最热排序
     */
    @Override
    public List<SortBuilder<?>> buildHottestSorts() {
        List<SortBuilder<?>> sorts = new ArrayList<>();
        sorts.add(SortBuilders.fieldSort("hotScore").order(SortOrder.DESC));
        sorts.add(SortBuilders.fieldSort("createdAt").order(SortOrder.DESC));
        sorts.add(SortBuilders.fieldSort("businessId").order(SortOrder.DESC));
        return sorts;
    }

    /**
     * 构建精选排序
     */
    @Override
    public List<SortBuilder<?>> buildSelectedSorts() {
        List<SortBuilder<?>> sorts = new ArrayList<>();
        sorts.add(SortBuilders.fieldSort("qualityScore").order(SortOrder.DESC));
        sorts.add(SortBuilders.fieldSort("createdAt").order(SortOrder.DESC));
        sorts.add(SortBuilders.fieldSort("businessId").order(SortOrder.DESC));
        return sorts;
    }

    /**
     * 同步笔记数据到Elasticsearch
     * 逻辑：
     * 1. 获取updateTime > lastUpdateTime的笔记（不过滤状态）
     * 2. 对于状态为PUBLISHED的笔记，保存到ES
     * 3. 对于状态不为PUBLISHED的笔记，从ES删除（处理删除、草稿等状态）
     */
    @Override
    public int syncNoteData(LocalDateTime updatedTime) {
        try {
            log.info("开始同步笔记数据: updatedTime={}", updatedTime);

            // 调用RPC获取笔记数据（不过滤状态）
            RpcResult<List<NoteSyncVo>> result = noteSearchDataService.syncNoteData(updatedTime);
            Assert.rpcResultOk(result);
            List<NoteSyncVo> noteSyncVos = result.getData();
            log.info("获取到笔记数据: 数量={}", noteSyncVos.size());

            // 2. 按状态分类：可发布的和不可发布的
            List<NoteSyncVo> publishedNotes = noteSyncVos.stream()
                    .filter(vo -> SocialNote.Status.PUBLISHED.equals(vo.getStatus()))
                    .toList();

            List<NoteSyncVo> unpublishedNotes = noteSyncVos.stream()
                    .filter(vo -> !SocialNote.Status.PUBLISHED.equals(vo.getStatus()))
                    .toList();

            int syncCount = 0;

            // 保存/修改PUBLISHED状态的笔记到ES
            if (!publishedNotes.isEmpty()) {
                List<UnifiedSearchDocument> documents = publishedNotes.stream()
                        .map(this::convertNoteSyncVOToDocument)
                        .filter(Objects::nonNull)
                        .toList();

                if (!documents.isEmpty()) {
                    elasticsearchOperations.save(documents);
                    log.info("保存笔记到ES: 成功条数={}", documents.size());
                    syncCount += documents.size();
                }
            }

            // 从ES删除非PUBLISHED状态的笔记
            if (!unpublishedNotes.isEmpty()) {
                unpublishedNotes.forEach(vo -> {
                    try {
                        String docId = SearchDocTypeEnum.NOTE.getIdPrefix() + vo.getNoteId();
                        elasticsearchOperations.delete(docId, UnifiedSearchDocument.class);
                        log.info("从ES删除笔记: noteId={}, status={}", vo.getNoteId(), vo.getStatus());
                    } catch (Exception e) {
                        log.error("删除ES文档失败: noteId={}", vo.getNoteId(), e);
                    }
                });
            }

            log.info("笔记数据同步完成: 保存数={}, 删除数={}", syncCount, unpublishedNotes.size());
            return syncCount;

        } catch (Exception e) {
            log.error("同步笔记数据异常: updatedTime={}", updatedTime, e);
            return 0;
        }
    }

    /**
     * 将NoteSyncVO转换为UnifiedSearchDocument
     */
    private UnifiedSearchDocument convertNoteSyncVOToDocument(NoteSyncVo vo) {
        try {
            if (vo == null || vo.getNoteId() == null) {
                return null;
            }

            // 计算热度分数
            Double hotScore = calculateHotScore(
                    vo.getLikeCount(),
                    vo.getCommentCount(),
                    vo.getCollectCount(),
                    vo.getCreatedAt()
            );

            return UnifiedSearchDocument.builder()
                    .id(SearchDocTypeEnum.NOTE.getIdPrefix() + vo.getNoteId())
                    .businessId(vo.getNoteId())
                    .dataType(SearchDocTypeEnum.NOTE.getValue())
                    .title(vo.getTitle())
                    .content(vo.getContent())
                    .tags(vo.getTags())
                    .creatorId(String.valueOf(vo.getUserId()))
                    .coverUrl(vo.getCoverUrl())
                    .likes(vo.getLikeCount())
                    .comments(vo.getCommentCount())
                    .saves(vo.getCollectCount())
                    .createdAt(vo.getCreatedAt())
                    .updatedAt(vo.getUpdatedAt())
                    .hotScore(hotScore)
                    .qualityScore(NoteSearchConstants.INITIAL_QUALITY_SCORE)
                    .noteMediaType(vo.getMediaType())
                    .build();

        } catch (Exception e) {
            log.error("转换NoteSyncVO为Document失败: noteId={}", vo.getNoteId(), e);
            return null;
        }
    }

    /**
     * 计算热度分数
     * 公式: likes * 1.0 + comments * 0.5 + collects * 2.0 + 时间衰减因子
     */
    @Override
    public Double calculateHotScore(Integer likeCount, Integer commentCount, Integer collectCount, LocalDateTime createdAt) {
        likeCount = likeCount == null ? 0 : likeCount;
        commentCount = commentCount == null ? 0 : commentCount;
        collectCount = collectCount == null ? 0 : collectCount;

        double baseScore = likeCount * NoteSearchConstants.LIKE_WEIGHT
                + commentCount * NoteSearchConstants.COMMENT_WEIGHT
                + collectCount * NoteSearchConstants.COLLECT_WEIGHT;

        // 时间衰减因子：距离现在时间越久，权重越低
        if (createdAt != null) {
            long hoursSinceCreated = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
            double timeDecay = Math.pow(NoteSearchConstants.TIME_DECAY_FACTOR, hoursSinceCreated / (double) NoteSearchConstants.TIME_DECAY_UNIT_HOURS);
            return baseScore * timeDecay;
        }

        return baseScore;
    }

}

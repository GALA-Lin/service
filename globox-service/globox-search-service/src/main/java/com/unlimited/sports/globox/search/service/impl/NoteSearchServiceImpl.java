package com.unlimited.sports.globox.search.service.impl;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.model.search.constants.NoteSearchConstants;
import com.unlimited.sports.globox.model.search.enums.NoteSortTypeEnum;
import com.unlimited.sports.globox.model.search.enums.SearchDocTypeEnum;
import com.unlimited.sports.globox.model.social.dto.NoteStatisticsDto;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.vo.NoteSyncVo;
import com.unlimited.sports.globox.model.social.vo.NoteItemVo;
import com.unlimited.sports.globox.dubbo.social.INoteSearchDataService;
import com.unlimited.sports.globox.search.document.NoteSearchDocument;
import com.unlimited.sports.globox.search.document.UnifiedSearchDocument;
import com.unlimited.sports.globox.search.document.UserSearchDocument;
import com.unlimited.sports.globox.search.service.INoteSearchService;
import com.unlimited.sports.globox.search.service.IUnifiedSearchService;
import com.unlimited.sports.globox.search.service.IUserSearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.elasticsearch.index.query.IdsQueryBuilder;

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

    private static final String HOT_SCORE_SCRIPT =
            "double likes = (doc['likes'].size() != 0 ? doc['likes'].value : 0);" +
            "double comments = (doc['comments'].size() != 0 ? doc['comments'].value : 0);" +
            "double saves = (doc['saves'].size() != 0 ? doc['saves'].value : 0);" +
            "double baseScore = likes * params.likeWeight + comments * params.commentWeight + saves * params.collectWeight;" +
            "if (doc.containsKey('createdAtMillis') && doc['createdAtMillis'].size() != 0) {" +
            "  long createdMillis = doc['createdAtMillis'].value;" +
            "  double hours = (params.nowMillis - createdMillis) / 3600000.0;" +
            "  if (hours < 0) { hours = 0; }" +
            "  double timeDecay = Math.pow(params.decayFactor, hours / params.decayUnit);" +
            "  return baseScore * timeDecay;" +
            "}" +
            "return baseScore;";

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @DubboReference(group = "rpc")
    private INoteSearchDataService noteSearchDataService;

    @Autowired
    private IUserSearchService userSearchService;

    @Lazy
    @Autowired
    private IUnifiedSearchService unifiedSearchService;

    /**
     * 搜索笔记 - 支持关键词、标签、排序
     *
     * @param keyword 搜索关键词 (会匹配 title 和 content)
     * @param tag 标签过滤
     * @param sortBy 排序方式: latest / hottest / selected
     * @param page 分页页码 (从1开始)
     * @param pageSize 每页大小
     * @param userId 当前登录用户ID (用于查询点赞状态)，可为null
     */
    @Override
    public PaginationResult<NoteItemVo> searchNotes(String keyword, String tag, String sortBy, Integer page, Integer pageSize, Long userId) {
        try {
            log.info("开始搜索笔记: keyword={}, tag={}, sortBy={}, page={}, pageSize={}",
                    keyword, tag, sortBy, page, pageSize);

            // 1. 参数验证和默认值设置
            page = (page == null || page <= 0) ? 1 : page;
            pageSize = (pageSize == null || pageSize <= 0) ? 10 : pageSize;
            NoteSortTypeEnum sortType = NoteSortTypeEnum.fromCode(sortBy);

            // 2. 构建查询条件
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery = buildCommonQuery(boolQuery, keyword);
            boolQuery = buildNoteSpecificQuery(boolQuery, tag);
            if (NoteSortTypeEnum.SELECTED.equals(sortType)) {
                boolQuery.filter(QueryBuilders.termQuery("featured", true));
            }

            // 3. 构建排序与查询（hotScore 在查询时动态计算，不在文档中维护）
            List<SortBuilder<?>> sorts;

            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
            if (NoteSortTypeEnum.HOTTEST.equals(sortType) || NoteSortTypeEnum.SELECTED.equals(sortType)) {
                Map<String, Object> params = new HashMap<>();
                params.put("nowMillis", System.currentTimeMillis());
                params.put("likeWeight", NoteSearchConstants.LIKE_WEIGHT);
                params.put("commentWeight", NoteSearchConstants.COMMENT_WEIGHT);
                params.put("collectWeight", NoteSearchConstants.COLLECT_WEIGHT);
                params.put("decayFactor", NoteSearchConstants.TIME_DECAY_FACTOR);
                params.put("decayUnit", NoteSearchConstants.TIME_DECAY_UNIT_HOURS.doubleValue());

                Script script = new Script(ScriptType.INLINE, "painless", HOT_SCORE_SCRIPT, params);
                FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders
                        .functionScoreQuery(boolQuery, ScoreFunctionBuilders.scriptFunction(script))
                        .boostMode(CombineFunction.REPLACE);
                queryBuilder.withQuery(functionScoreQuery);

                sorts = buildHottestSorts();
            } else {
                queryBuilder.withQuery(boolQuery);
                sorts = buildSortBuilders(sortType.getCode());
            }

            // 4. 构建分页对象（ES中page从0开始）
            Pageable pageable = PageRequest.of(page - 1, pageSize);

            sorts.forEach(queryBuilder::withSorts);
            queryBuilder.withPageable(pageable);

            // 6. 执行查询
            NativeSearchQuery nativeSearchQuery = queryBuilder.build();
            SearchHits<NoteSearchDocument> searchHits = elasticsearchOperations.search(
                    nativeSearchQuery,
                    NoteSearchDocument.class
            );

            log.info("笔记搜索结果: 命中数={}, 总数={}", searchHits.getSearchHits().size(), searchHits.getTotalHits());

            // 7. 提取查询结果
            List<NoteSearchDocument> docs = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .toList();

            // 8. 提取所有userId，从本地ES批量获取用户信息
            List<Long> userIds = docs.stream()
                    .map(NoteSearchDocument::getUserId)
                    .distinct()
                    .toList();

            Map<Long, UserSearchDocument> userMap = userSearchService.getUsersByIds(userIds);

            // 9. 查询用户点赞状态（点赞数/评论数直接使用ES中已同步的数据）
            List<Long> noteIds = docs.stream()
                    .map(NoteSearchDocument::getNoteId)
                    .toList();

            Set<Long> likedNoteIds = Collections.emptySet();
            if (userId != null && userId > 0) {
                try {
                    RpcResult<Set<Long>> likedResult = noteSearchDataService.queryUserLikedNoteIds(userId, noteIds);
                    if (likedResult.isSuccess() && likedResult.getData() != null) {
                        likedNoteIds = likedResult.getData();
                    }
                } catch (Exception e) {
                    log.warn("查询用户点赞状态异常: userId={}", userId, e);
                }
            }

            // 10. 转换为NoteItemVo并填充用户信息和统计数据
            final Set<Long> finalLikedNoteIds = likedNoteIds;
            List<NoteItemVo> noteItems = docs.stream()
                    .map(doc -> {
                        try {
                            UserSearchDocument userDoc = userMap.get(doc.getUserId());
                            boolean isLiked = finalLikedNoteIds.contains(doc.getNoteId());
                            int likeCount = doc.getLikes() != null ? doc.getLikes() : 0;
                            // 用户已点赞但ES数据未同步时，保证至少显示1
                            if (isLiked && likeCount < 1) {
                                likeCount = 1;
                            }
                            NoteStatisticsDto stats = NoteStatisticsDto.builder()
                                    .noteId(doc.getNoteId())
                                    .likeCount(likeCount)
                                    .commentCount(doc.getComments() != null ? doc.getComments() : 0)
                                    .isLiked(isLiked)
                                    .build();
                            return toListItemVo(doc, userDoc, stats);
                        } catch (Exception e) {
                            log.error("笔记转换失败: noteId={}", doc.getNoteId(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            // 11. 返回分页结果
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
        sorts.add(SortBuilders.fieldSort("noteId").order(SortOrder.DESC));
        return sorts;
    }

    /**
     * 构建最热排序
     */
    @Override
    public List<SortBuilder<?>> buildHottestSorts() {
        List<SortBuilder<?>> sorts = new ArrayList<>();
        sorts.add(SortBuilders.scoreSort().order(SortOrder.DESC));
        sorts.add(SortBuilders.fieldSort("createdAt").order(SortOrder.DESC));
        sorts.add(SortBuilders.fieldSort("noteId").order(SortOrder.DESC));
        return sorts;
    }

    /**
     * 构建精选排序
     */
    @Override
    public List<SortBuilder<?>> buildSelectedSorts() {
        return buildHottestSorts();
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
                List<NoteSearchDocument> documents = publishedNotes.stream()
                        .map(this::convertNoteSyncVOToDocument)
                        .filter(Objects::nonNull)
                        .toList();

                if (!documents.isEmpty()) {
                    elasticsearchOperations.save(documents);
                    log.info("保存笔记到ES: 成功条数={}", documents.size());
                    syncCount += documents.size();

                    // 同步到统一索引
                    List<UnifiedSearchDocument> unifiedDocs = documents.stream()
                            .map(note -> UnifiedSearchDocument.builder()
                                    .id(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.NOTE, note.getNoteId()))
                                    .businessId(note.getNoteId())
                                    .dataType(SearchDocTypeEnum.NOTE.getValue())
                                    .title(note.getTitle())
                                    .content(note.getContent())
                                    .tags(note.getTags())
                                    .location(null)
                                    .region(null)
                                    .coverUrl(note.getCoverUrl())
                                    .score(Optional.ofNullable(calculateHotScore(
                                            note.getLikes(),
                                            note.getComments(),
                                            note.getSaves(),
                                            note.getCreatedAt()
                                    )).orElse(0.0))
                                    .createdAt(note.getCreatedAt())
                                    .updatedAt(note.getUpdatedAt())
                                    .build())
                            .collect(Collectors.toList());
                    unifiedSearchService.saveOrUpdateToUnified(unifiedDocs);
                }
            }

            // 从ES删除非PUBLISHED状态的笔记
            if (!unpublishedNotes.isEmpty()) {
                List<String> docIds = unpublishedNotes.stream()
                        .map(vo -> SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.NOTE, vo.getNoteId()))
                        .collect(Collectors.toList());

                // 批量删除笔记索引中的文档（基于查询的批量删除）
                IdsQueryBuilder idsQuery = new IdsQueryBuilder().addIds(docIds.toArray(new String[0]));
                NativeSearchQuery deleteQuery = new NativeSearchQueryBuilder()
                        .withQuery(idsQuery)
                        .build();
                elasticsearchOperations.delete(deleteQuery, NoteSearchDocument.class);
                log.info("从ES笔记索引删除笔记: 删除数={}", unpublishedNotes.size());

                // 从统一索引删除
                unifiedSearchService.deleteFromUnified(docIds);
            }

            log.info("笔记数据同步完成: 保存数={}, 删除数={}", syncCount, unpublishedNotes.size());
            return syncCount;

        } catch (Exception e) {
            log.error("同步笔记数据异常: updatedTime={}", updatedTime, e);
            return 0;
        }
    }

    /**
     * 将NoteSyncVO转换为NoteSearchDocument
     */
    private NoteSearchDocument convertNoteSyncVOToDocument(NoteSyncVo vo) {
        try {
            if (vo == null || vo.getNoteId() == null) {
                return null;
            }

            return NoteSearchDocument.builder()
                    .id(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.NOTE,vo.getNoteId()))
                    .noteId(vo.getNoteId())
                    .title(vo.getTitle())
                    .content(vo.getContent())
                    .tags(vo.getTags())
                    .userId(vo.getUserId())
                    .coverUrl(vo.getCoverUrl())
                    .likes(vo.getLikeCount())
                    .comments(vo.getCommentCount())
                    .saves(vo.getCollectCount())
                    .createdAt(vo.getCreatedAt())
                    .createdAtMillis(vo.getCreatedAt() != null
                            ? vo.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            : null)
                    .updatedAt(vo.getUpdatedAt())
                    .qualityScore(NoteSearchConstants.INITIAL_QUALITY_SCORE)
                    .mediaType(vo.getMediaType())
                    .featured(vo.getFeatured() != null ? vo.getFeatured() : false)
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

    /**
     * 获取精选笔记 - 支持随机排序和分页
     */
    @Override
    public PaginationResult<NoteItemVo> getFeaturedNotes(Integer page, Integer pageSize, Long seed, Long userId) {
        try {
            log.info("开始获取精选笔记: page={}, pageSize={}, seed={}, userId={}", page, pageSize, seed, userId);

            // 1. 参数验证和默认值设置
            page = (page == null || page <= 0) ? 1 : page;
            pageSize = (pageSize == null || pageSize <= 0) ? 10 : pageSize;
            seed = (seed == null) ? System.currentTimeMillis() : seed;

            // 2. 构建基础过滤条件
            BoolQueryBuilder filterQuery = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termQuery("featured", true));

            // 3. 使用 function_score 实现基于 seed 的随机排序
            FunctionScoreQueryBuilder functionScoreQuery =
                QueryBuilders.functionScoreQuery(filterQuery,
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder[] {
                        new org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder.FilterFunctionBuilder(
                            ScoreFunctionBuilders.randomFunction()
                                .seed(seed)
                                .setField("_seq_no")
                        )
                    });

            // 4. 构建排序 - 按 score 降序（随机分数）
            List<SortBuilder<?>> sorts = new ArrayList<>();
            sorts.add(SortBuilders.scoreSort().order(SortOrder.DESC));
            sorts.add(SortBuilders.fieldSort("noteId").order(SortOrder.DESC));

            // 5. 构建分页对象
            Pageable pageable = PageRequest.of(page - 1, pageSize);

            // 6. 构建查询对象
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                    .withQuery(functionScoreQuery);

            sorts.forEach(queryBuilder::withSorts);
            queryBuilder.withPageable(pageable);

            // 7. 执行查询
            NativeSearchQuery nativeSearchQuery = queryBuilder.build();
            SearchHits<NoteSearchDocument> searchHits = elasticsearchOperations.search(
                    nativeSearchQuery,
                    NoteSearchDocument.class
            );

            log.info("精选笔记查询结果: 命中数={}, 总数={}", searchHits.getSearchHits().size(), searchHits.getTotalHits());

            // 8. 提取查询结果
            List<NoteSearchDocument> docs = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .toList();

            // 9. 提取所有userId，从本地ES批量获取用户信息
            List<Long> userIds = docs.stream()
                    .map(NoteSearchDocument::getUserId)
                    .distinct()
                    .toList();

            Map<Long, UserSearchDocument> userMap = userSearchService.getUsersByIds(userIds);

            // 10. 查询用户点赞状态（点赞数/评论数直接使用ES中已同步的数据）
            List<Long> noteIds = docs.stream()
                    .map(NoteSearchDocument::getNoteId)
                    .toList();

            Set<Long> likedNoteIds = Collections.emptySet();
            if (userId != null && userId > 0) {
                try {
                    RpcResult<Set<Long>> likedResult = noteSearchDataService.queryUserLikedNoteIds(userId, noteIds);
                    if (likedResult.isSuccess() && likedResult.getData() != null) {
                        likedNoteIds = likedResult.getData();
                    }
                } catch (Exception e) {
                    log.warn("查询用户点赞状态异常: userId={}", userId, e);
                }
            }

            // 11. 转换为NoteItemVo并填充用户信息和统计数据
            final Set<Long> finalLikedNoteIds = likedNoteIds;
            List<NoteItemVo> noteItems = docs.stream()
                    .map(doc -> {
                        try {
                            UserSearchDocument userDoc = userMap.get(doc.getUserId());
                            boolean isLiked = finalLikedNoteIds.contains(doc.getNoteId());
                            int likeCount = doc.getLikes() != null ? doc.getLikes() : 0;
                            if (isLiked && likeCount < 1) {
                                likeCount = 1;
                            }
                            NoteStatisticsDto stats = NoteStatisticsDto.builder()
                                    .noteId(doc.getNoteId())
                                    .likeCount(likeCount)
                                    .commentCount(doc.getComments() != null ? doc.getComments() : 0)
                                    .isLiked(isLiked)
                                    .build();
                            return toListItemVo(doc, userDoc, stats);
                        } catch (Exception e) {
                            log.error("笔记转换失败: noteId={}", doc.getNoteId(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            // 12. 返回分页结果
            return PaginationResult.build(noteItems, searchHits.getTotalHits(), page, pageSize);

        } catch (Exception e) {
            log.error("获取精选笔记异常", e);
            return PaginationResult.build(Collections.emptyList(), 0, page == null ? 1 : page, pageSize == null ? 10 : pageSize);
        }
    }

    /**
     * 将 NoteSearchDocument 转换为 NoteItemVo
     *
     * @param doc NoteSearchDocument
     * @param userDoc UserSearchDocument
     * @param stats NoteStatisticsDto
     */
    @Override
    public NoteItemVo toListItemVo(NoteSearchDocument doc, UserSearchDocument userDoc, NoteStatisticsDto stats) {
        NoteItemVo noteItem = new NoteItemVo();
        noteItem.setNoteId(doc.getNoteId());
        noteItem.setUserId(doc.getUserId());
        noteItem.setNickName(userDoc != null ? userDoc.getNickName() : null);
        noteItem.setAvatarUrl(userDoc != null ? userDoc.getAvatarUrl() : null);
        noteItem.setTitle(doc.getTitle());

        String content = doc.getContent();
        if (content != null && content.length() > 150) {
            content = content.substring(0, 150) + "...";
        }
        noteItem.setContent(content);

        noteItem.setCoverUrl(doc.getCoverUrl());
        noteItem.setMediaType(doc.getMediaType() != null ? doc.getMediaType() : "IMAGE");
        noteItem.setLikeCount(stats.getLikeCount());
        noteItem.setCommentCount(stats.getCommentCount());
        noteItem.setStatus(doc.getStatus() != null ? String.valueOf(doc.getStatus()) : "PUBLISHED");
        noteItem.setCreatedAt(doc.getCreatedAt());
        noteItem.setLiked(stats.getIsLiked());
        return noteItem;
    }
}

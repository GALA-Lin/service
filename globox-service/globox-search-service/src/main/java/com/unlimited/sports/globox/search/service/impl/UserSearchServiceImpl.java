package com.unlimited.sports.globox.search.service.impl;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.user.IUserSearchDataService;
import com.unlimited.sports.globox.dubbo.social.ISocialFollowDataService;
import com.unlimited.sports.globox.model.auth.vo.UserListItemVo;
import com.unlimited.sports.globox.model.auth.vo.UserSyncVo;
import com.unlimited.sports.globox.model.search.enums.SearchDocTypeEnum;
import com.unlimited.sports.globox.search.document.UserSearchDocument;
import com.unlimited.sports.globox.search.document.UnifiedSearchDocument;
import com.unlimited.sports.globox.search.service.IUserSearchService;
import com.unlimited.sports.globox.search.service.IUnifiedSearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户搜索服务实现
 * 用于用户搜索（通过昵称、球盒号搜索）
 */
@Slf4j
@Service
public class UserSearchServiceImpl implements IUserSearchService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @DubboReference(group = "rpc")
    private IUserSearchDataService userSearchDataService;

    @DubboReference(group = "rpc")
    private ISocialFollowDataService socialFollowDataService;

    @Lazy
    @Autowired
    private IUnifiedSearchService unifiedSearchService;

    @Override
    public PaginationResult<UserListItemVo> searchUsers(String keyword, Integer page, Integer pageSize) {
        try {
            log.info("搜索用户: keyword={}, page={}, pageSize={}", keyword, page, pageSize);

            // 参数默认值
            page = (page == null || page <= 0) ? 1 : page;
            pageSize = (pageSize == null || pageSize <= 0) ? 20 : pageSize;

            // 构建查询
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // 关键词搜索（匹配昵称或球盒号）
            if (keyword != null && !keyword.trim().isEmpty()) {
                String trimmedKeyword = keyword.trim();
                boolQuery.must(
                    QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery("nickName", keyword).boost(2.0f))
                        .should(QueryBuilders.prefixQuery("globoxNo", trimmedKeyword).boost(3.0f))
                        .should(QueryBuilders.wildcardQuery("globoxNo", "*" + trimmedKeyword + "*"))
                        .minimumShouldMatch(1)
                );
            }

            // 构建查询对象
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                    .withQuery(boolQuery)
                    .withSorts(SortBuilders.scoreSort())
                    .withPageable(pageable)
                    .build();

            // 执行查询
            SearchHits<UserSearchDocument> searchHits = elasticsearchOperations.search(
                    nativeSearchQuery,
                    UserSearchDocument.class
            );

            log.info("用户搜索结果: 命中数={}, 总数={}", searchHits.getSearchHits().size(), searchHits.getTotalHits());

            // 提取结果并转换为ListItemVo
            List<UserListItemVo> users = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(this::toListItemVo)
                    .toList();

            // 批量获取粉丝数量
            if (!users.isEmpty()) {
                List<Long> userIds = users.stream()
                        .map(UserListItemVo::getUserId)
                        .toList();
                enrichWithFollowers(users, userIds);
            }

            // 构建分页结果
            long total = searchHits.getTotalHits();
            return PaginationResult.build(users, total, page, pageSize);
        } catch (Exception e) {
            log.error("用户搜索异常: keyword={}", keyword, e);
            return PaginationResult.build(Collections.emptyList(),0,page,pageSize);
        }
    }

    @Override
    public int syncUserData(LocalDateTime updatedTime) {
        try {
            log.info("开始同步用户数据: updatedTime={}", updatedTime);

            // 通过RPC获取用户数据
            RpcResult<List<UserSyncVo>> result = userSearchDataService.syncUserData(updatedTime);
            Assert.rpcResultOk(result);
            List<UserSyncVo> userSyncVos = result.getData();
            log.info("获取到用户数据: 数量={}", userSyncVos.size());
            if (userSyncVos.isEmpty()) {
                return 0;
            }
            // 转换为UserSearchDocument并保存到ES
            List<UserSearchDocument> documents = userSyncVos.stream()
                    .map(this::convertUserSyncVOToDocument)
                    .filter(Objects::nonNull)
                    .toList();
            if (documents.isEmpty()) {
                log.info("转换后没有有效的文档");
                return 0;
            }
            elasticsearchOperations.save(documents);
            log.info("用户数据同步完成: 成功条数={}", documents.size());

            // 同步到统一索引
            List<UnifiedSearchDocument> unifiedDocs = documents.stream()
                    .map(user -> UnifiedSearchDocument.builder()
                            .id(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.USER, user.getUserId()))
                            .businessId(user.getUserId())
                            .dataType(SearchDocTypeEnum.USER.getValue())
                            .title(user.getNickName())
                            .content(null)
                            .tags(null)
                            .location(null)
                            .region(null)
                            .coverUrl(user.getAvatarUrl())
                            .score(user.getNtrp() != null ? user.getNtrp().doubleValue() : 0.0)
                            .createdAt(null)
                            .updatedAt(null)
                            .build())
                    .collect(Collectors.toList());
            unifiedSearchService.saveOrUpdateToUnified(unifiedDocs);

            return documents.size();

        } catch (Exception e) {
            log.error("同步用户数据异常: updatedTime={}", updatedTime, e);
            return 0;
        }
    }

    /**
     * 将UserSyncVo转换为UserSearchDocument
     */
    private UserSearchDocument convertUserSyncVOToDocument(UserSyncVo vo) {
        try {
            if (vo == null || vo.getUserId() == null) {
                return null;
            }

            return UserSearchDocument.builder()
                    .id(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.USER, vo.getUserId()))
                    .userId(vo.getUserId())
                    .nickName(vo.getNickName())
                    .globoxNo(vo.getGloboxNo())
                    .avatarUrl(vo.getAvatarUrl())
                    .gender(vo.getGender())
                    .ntrp(vo.getNtrp())
                    .build();

        } catch (Exception e) {
            log.error("转换UserSyncVo为Document失败: userId={}", vo.getUserId(), e);
            return null;
        }
    }

    @Override
    public Map<Long, UserSearchDocument> getUsersByIds(List<Long> userIds) {
        Map<Long, UserSearchDocument> result = new HashMap<>();
        
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        try {
            List<String> docIds = userIds.stream()
                    .map(id -> SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.USER,id))
                    .toList();

            NativeSearchQuery query = new NativeSearchQueryBuilder()
                    .withIds(docIds)
                    .build();

            List<MultiGetItem<UserSearchDocument>> items = elasticsearchOperations.multiGet(
                    query,
                    UserSearchDocument.class,
                    elasticsearchOperations.getIndexCoordinatesFor(UserSearchDocument.class)
            );

            items.stream()
                    .filter(item -> item != null && item.hasItem() && item.getItem() != null && item.getItem().getUserId() != null)
                    .map(MultiGetItem::getItem)
                    .forEach(doc -> result.put(doc.getUserId(), doc));

            log.debug("批量获取用户详情: 请求数={}, 获取数={}", userIds.size(), result.size());
        } catch (Exception e) {
            log.error("批量获取用户详情异常: userIds={}", userIds, e);
        }

        return result;
    }

    @Override
    public UserSearchDocument getUserById(Long userId) {
        if (userId == null) {
            return null;
        }

        Map<Long, UserSearchDocument> result = getUsersByIds(List.of(userId));
        return result.get(userId);
    }

    @Override
    public UserListItemVo toListItemVo(UserSearchDocument document) {
        if (document == null) {
            return null;
        }
        return UserListItemVo.builder()
                .userId(document.getUserId())
                .globoxNo(document.getGloboxNo())
                .nickName(document.getNickName())
                .avatarUrl(document.getAvatarUrl())
                .gender(document.getGender())
                .ntrp(document.getNtrp())
                .build();
    }

    /**
     * 批量获取粉丝数量并填充到UserListItemVo
     */
    private void enrichWithFollowers(List<UserListItemVo> users, List<Long> userIds) {
        try {
            RpcResult<Map<Long, Integer>> result = socialFollowDataService.getUserFollowerCounts(userIds);
            if (result != null && result.isSuccess() && result.getData() != null) {
                Map<Long, Integer> followerCounts = result.getData();
                users.forEach(user -> {
                    Integer followerCount = followerCounts.getOrDefault(user.getUserId(), 0);
                    user.setFollowers(followerCount);
                });
                log.debug("批量获取粉丝数量完成: 用户数={}", users.size());
            } else {
                log.warn("批量获取粉丝数量失败, 使用默认值0");
                users.forEach(user -> user.setFollowers(0));
            }
        } catch (Exception e) {
            log.error("批量获取粉丝数量异常: userIds={}", userIds, e);
            users.forEach(user -> user.setFollowers(0));
        }
    }
}

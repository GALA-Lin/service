package com.unlimited.sports.globox.search.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.model.social.dto.NoteStatisticsDto;
import com.unlimited.sports.globox.model.social.vo.NoteItemVo;
import com.unlimited.sports.globox.search.document.NoteSearchDocument;
import com.unlimited.sports.globox.search.document.UserSearchDocument;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记搜索服务接口
 * 处理笔记专用的搜索、过滤、排序逻辑
 *
 * 排序策略：
 * 1. latest - 最新发布 (按 createdAt 降序)
 * 2. hottest - 最热门 (按 hotScore 降序)
 * 3. selected - 精选推荐 (按 qualityScore 降序)
 */
public interface INoteSearchService {

    /**
     * 搜索笔记 - 支持关键词、标签、排序
     *
     * @param keyword 搜索关键词 (会匹配 title 和 content)
     * @param tag 笔记标签过滤 (单个标签)
     * @param sortBy 排序方式: latest / hottest / selected
     * @param page 分页页码 (从1开始)
     * @param pageSize 每页大小
     * @param userId 当前登录用户ID (用于查询点赞状态)，可为null
     * @return 笔记搜索结果 PaginationResult<NoteItemVo>
     */
    PaginationResult<NoteItemVo> searchNotes(String keyword, String tag, String sortBy, Integer page, Integer pageSize, Long userId);

    /**
     * 构建公共查询条件
     * 包括: dataType = NOTE, keyword(title/content)
     *
     * @param boolQuery bool查询对象
     * @param keyword 关键词
     * @return 构建后的boolQuery
     */
    BoolQueryBuilder buildCommonQuery(BoolQueryBuilder boolQuery, String keyword);

    /**
     * 构建笔记专属过滤条件
     * 包括: tag (标签过滤)
     *
     * @param boolQuery bool查询对象
     * @param tag 单个标签
     * @return 构建后的boolQuery
     */
    BoolQueryBuilder buildNoteSpecificQuery(BoolQueryBuilder boolQuery, String tag);

    /**
     * 构建排序参数
     *
     * @param sortBy 排序方式: latest / hottest / selected
     * @return 排序列表
     */
    List<SortBuilder<?>> buildSortBuilders(String sortBy);

    /**
     * 构建最新排序
     * 按创建时间降序
     *
     * @return 排序列表
     */
    List<SortBuilder<?>> buildLatestSorts();

    /**
     * 构建最热排序
     * 按热度分数降序，secondary按创建时间降序
     *
     * @return 排序列表
     */
    List<SortBuilder<?>> buildHottestSorts();

    /**
     * 构建精选排序
     * 按质量分数降序，secondary按创建时间降序
     *
     * @return 排序列表
     */
    List<SortBuilder<?>> buildSelectedSorts();

    /**
     * 同步笔记数据到Elasticsearch
     *
     * @param updatedTime 上次同步时间，为null则全量同步
     * @return 同步的数据条数
     */
    int syncNoteData(LocalDateTime updatedTime);

    /**
     * 计算热度分数
     * 公式: likes * 1.0 + comments * 0.5 + collects * 2.0 + 时间衰减因子
     *
     * @param likeCount 点赞数
     * @param commentCount 评论数
     * @param collectCount 收藏数
     * @param createdAt 创建时间
     * @return 计算出的热度分数
     */
    Double calculateHotScore(Integer likeCount, Integer commentCount, Integer collectCount, LocalDateTime createdAt);

    /**
     * 将NoteSearchDocument转换为NoteItemVo
     * @param document 笔记搜索文档
     * @param userDocument 用户搜索文档（包含用户昵称和头像）
     * @param stats 笔记统计信息（包含点赞数、评论数、用户是否点赞）
     * @return 笔记列表项视图对象
     */
    NoteItemVo toListItemVo(NoteSearchDocument document, UserSearchDocument userDocument, NoteStatisticsDto stats);

    /**
     * 获取精选笔记 - 支持随机排序和分页
     *
     * @param page 分页页码 (从1开始)
     * @param pageSize 每页大小
     * @param seed 随机种子（用于保证分页一致性，翻页时使用相同的seed，刷新时使用新的seed）
     * @param userId 当前登录用户ID (用于查询点赞状态)，可为null
     * @return 精选笔记搜索结果 PaginationResult<NoteItemVo>
     */
    PaginationResult<NoteItemVo> getFeaturedNotes(Integer page, Integer pageSize, Long seed, Long userId);
}

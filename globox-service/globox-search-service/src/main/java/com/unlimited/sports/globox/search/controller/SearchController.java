package com.unlimited.sports.globox.search.controller;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.search.dto.SearchNotesDto;
import com.unlimited.sports.globox.model.social.vo.NoteItemVo;
import com.unlimited.sports.globox.model.venue.dto.GetVenueListDto;
import com.unlimited.sports.globox.model.venue.vo.VenueListResponse;
import com.unlimited.sports.globox.model.search.dto.UnifiedSearchResultVo;
import com.unlimited.sports.globox.model.auth.vo.UserListItemVo;
import com.unlimited.sports.globox.search.service.IVenueSearchService;
import com.unlimited.sports.globox.search.service.INoteSearchService;
import com.unlimited.sports.globox.search.service.IUserSearchService;
import com.unlimited.sports.globox.search.service.impl.UnifiedSearchServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索控制器
 * 提供基于Elasticsearch的搜索API
 */
@Slf4j
@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private IVenueSearchService venueSearchService;

    @Autowired
    private INoteSearchService noteSearchService;


    @Autowired
    private UnifiedSearchServiceImpl unifiedSearchService;

    @Autowired
    private IUserSearchService userSearchService;


    /**
     * 统一搜索
     * @param keyword 搜索关键词
     * @param types 类型过滤（可选，如 VENUE,NOTE,USER）
     * @param sortBy 排序方式：relevance(相关性,默认) | score(热度) | time(时间)
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 混合搜索结果
     */
    @GetMapping("/unified")
    public R<UnifiedSearchResultVo> unifiedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> types,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("统一搜索: keyword={}, types={}, sortBy={}, page={}, pageSize={}",
                keyword, types, sortBy, page, pageSize);
        UnifiedSearchResultVo result = unifiedSearchService.searchUnified(keyword, types, sortBy, page, pageSize);
        return R.ok(result);
    }


    /**
     * 搜索场馆
     * @param dto 查询条件
     * @return 场馆列表和筛选选项
     */
    @PostMapping("/venues")
    public R<VenueListResponse> searchVenues(@Valid @RequestBody GetVenueListDto dto) {
        log.info("搜索场馆: {}", dto);
        VenueListResponse result = venueSearchService.searchVenues(dto);
        return R.ok(result);
    }

    /**
     * todo 暂时手动通过接口同步,后续转为定时器
     * 增量同步场馆数据到ES
     * @param updatedTime 上次同步时间
     */
    @PostMapping("/venues/sync")
    public R<Integer> syncVenues(@RequestParam(required = false) LocalDateTime updatedTime) {
        log.info("同步场馆数据: updatedTime={}", updatedTime);
        int count = venueSearchService.syncVenueData(updatedTime);
        log.info("场馆同步完成: 同步数={}", count);
        return R.ok(count);
    }


    /**
     * 搜索笔记
     * @param dto 搜索条件
     * @return 笔记搜索结果
     */
    @GetMapping("/notes")
    public R<PaginationResult<NoteItemVo>> searchNotes(@Valid SearchNotesDto dto,
                                                       @RequestHeader("X-User-Id") Long userId) {
        log.info("搜索笔记: keyword={}, tag={}, sortBy={}", dto.getKeyword(), dto.getTag(), dto.getSortBy());
        PaginationResult<NoteItemVo> result = noteSearchService.searchNotes(
                dto.getKeyword(), dto.getTag(), dto.getSortBy(), dto.getPage(), dto.getPageSize(),userId);
        return R.ok(result);
    }

    /**
     * 获取精选笔记
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @param seed 随机种子（用于保证分页一致性，翻页时使用相同的seed，刷新时使用新的seed）
     * @return 精选笔记列表
     */
    @GetMapping("/notes/featured")
    public R<PaginationResult<NoteItemVo>> getFeaturedNotes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long seed,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        log.info("获取精选笔记: page={}, pageSize={}, seed={}, userId={}", page, pageSize, seed, userId);
        PaginationResult<NoteItemVo> result = noteSearchService.getFeaturedNotes(page, pageSize, seed, userId);
        return R.ok(result);
    }

    /**
     * todo 暂时手动通过接口同步,后续转为定时器
     * 增量同步笔记数据到ES
     * @param updatedTime 上次同步时间
     */
    @PostMapping("/notes/sync")
    public R<Integer> syncNotes(@RequestParam(required = false) LocalDateTime updatedTime) {
        log.info("同步笔记数据: updatedTime={}", updatedTime);
        int count = noteSearchService.syncNoteData(updatedTime);
        log.info("笔记同步完成: 同步数={}", count);
        return R.ok(count);
    }


    /**
     *
     * 搜索用户（通过昵称或球盒号）
     * @param keyword 搜索关键词
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 用户搜索结果
     */
    @GetMapping("/users")
    public R<PaginationResult<UserListItemVo>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("搜索用户: keyword={}", keyword);
        PaginationResult<UserListItemVo> result = userSearchService.searchUsers(keyword, page, pageSize);
        return R.ok(result);
    }

    /**
     * todo 暂时手动通过接口同步,后续转为定时器
     * 增量同步用户数据到ES
     * @param updatedTime 上次同步时间
     */
    @PostMapping("/users/sync")
    public R<Integer> syncUsers(@RequestParam(required = false) LocalDateTime updatedTime) {
        log.info("同步用户数据: updatedTime={}", updatedTime);
        int count = userSearchService.syncUserData(updatedTime);
        log.info("用户同步完成: 同步数={}", count);
        return R.ok(count);
    }
}

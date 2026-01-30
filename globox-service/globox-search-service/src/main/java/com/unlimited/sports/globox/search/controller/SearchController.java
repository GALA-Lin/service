package com.unlimited.sports.globox.search.controller;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.search.dto.SearchNotesDto;
import com.unlimited.sports.globox.model.social.vo.NoteItemVo;
import com.unlimited.sports.globox.model.venue.dto.GetVenueListDto;
import com.unlimited.sports.globox.model.venue.vo.VenueListResponse;
import com.unlimited.sports.globox.search.service.IVenueSearchService;
import com.unlimited.sports.globox.search.service.INoteSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一搜索控制器
 * 提供基于Elasticsearch的搜索API入口
 */
@Slf4j
@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private IVenueSearchService venueSearchService;

    @Autowired
    private INoteSearchService noteSearchService;

    /**
     * 搜索场馆（基于ES）
     * @param dto 查询条件
     * @return 场馆列表和筛选选项
     */
    @PostMapping("/venues")
    public R<VenueListResponse> searchVenues(@Valid @RequestBody GetVenueListDto dto) {
        log.info("ES场馆搜索:{}", dto);
        VenueListResponse result = venueSearchService.searchVenues(dto);
        return R.ok(result);
    }



    /**
     * 增量同步场馆数据到ES
     * @param updatedTime 上次同步时间（格式：yyyy-MM-dd HH:mm:ss）
     * @return 同步的数据条数
     */
    @PostMapping("/venues/sync/incremental")
    public R<Void> syncVenuesIncremental(@RequestParam(required = false) LocalDateTime updatedTime) {
        log.info("开始增量同步场馆数据: updatedTime={}", updatedTime);

        int count = venueSearchService.syncVenueData(updatedTime);

        log.info("增量同步完成: updatedTime={}, 同步数={}", updatedTime, count);
        return R.ok();
    }

    /**
     * 搜索笔记（基于ES）
     * @param dto 搜索条件
     * @return 笔记搜索结果
     */
    @GetMapping("/notes")
    public R<PaginationResult<NoteItemVo>> searchNotes(@Valid SearchNotesDto dto) {
        log.info("搜索笔记: keyword={}, tag={}, sortBy={}, page={}, pageSize={}",
                dto.getKeyword(), dto.getTag(), dto.getSortBy(), dto.getPage(), dto.getPageSize());

        PaginationResult<NoteItemVo> result = noteSearchService.searchNotes(
                dto.getKeyword(),
                dto.getTag(),
                dto.getSortBy(),
                dto.getPage(),
                dto.getPageSize()
        );

        return R.ok(result);
    }

    /**
     * 增量同步笔记数据到ES
     * @param updatedTime 上次同步时间（格式：yyyy-MM-dd HH:mm:ss）
     * @return 同步的数据条数
     */
    @PostMapping("/notes/sync/incremental")
    public R<Void> syncNotesIncremental(@RequestParam(required = false) LocalDateTime updatedTime) {
        log.info("开始增量同步笔记数据: updatedTime={}", updatedTime);

        int count = noteSearchService.syncNoteData(updatedTime);

        log.info("增量同步完成: updatedTime={}, 同步数={}", updatedTime, count);
        return R.ok();
    }
}

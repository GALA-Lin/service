package com.unlimited.sports.globox.search.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.venue.dto.GetVenueListDto;
import com.unlimited.sports.globox.model.venue.vo.VenueListResponse;
import com.unlimited.sports.globox.search.service.IVenueSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
}

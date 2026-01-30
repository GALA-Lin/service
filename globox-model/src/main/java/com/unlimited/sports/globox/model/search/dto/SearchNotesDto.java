package com.unlimited.sports.globox.model.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 笔记搜索请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchNotesDto {

    /**
     * 搜索关键词（匹配标题和内容）
     */
    private String keyword;

    /**
     * 笔记标签（单个标签）
     */
    private String tag;

    /**
     * 排序方式：latest(最新) / hottest(最热) / selected(精选)
     */
    private String sortBy;

    /**
     * 分页页码（从1开始）
     */
    @Min(value = 1, message = "页码最小值为1")
    private Integer page;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小最小值为1")
    @Max(value = 50, message = "每页大小最大值为50")
    private Integer pageSize;
}

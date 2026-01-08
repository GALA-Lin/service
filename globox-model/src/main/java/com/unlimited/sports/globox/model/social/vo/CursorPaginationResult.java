package com.unlimited.sports.globox.model.social.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 游标分页结果
 */
@Data
@Schema(description = "游标分页结果")
public class CursorPaginationResult<T> {

    @Schema(description = "数据列表", example = "[]")
    private List<T> list;

    @Schema(description = "下一页游标（null表示没有更多数据）", example = "2025-12-28T10:00:00|123")
    private String nextCursor;

    @Schema(description = "是否还有更多数据", example = "true")
    private Boolean hasMore;
}


package com.unlimited.sports.globox.common.result;

import lombok.Data;

import java.util.List;


/**
 * @Description: 分页响应结果
 */
@Data
public class PaginationResult<T> {
    private List<T> list;

    private long total;

    private int page;

    private int pageSize;

    private int totalPages;

    /**
     * 是否有更多数据（用于游标分页或无限滚动场景）
     */
    private Boolean hasMore;



    public PaginationResult(List<T> list, long total, int page, int pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
        this.hasMore = page < this.totalPages;
    }

    public static <T> PaginationResult<T> build(List<T> items, long total, int page, int pageSize) {
        return new PaginationResult<>(items, total, page, pageSize);
    }
}

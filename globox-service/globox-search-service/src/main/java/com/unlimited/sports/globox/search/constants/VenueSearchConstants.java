package com.unlimited.sports.globox.search.constants;

import java.math.BigDecimal;

/**
 * 场馆搜索常量
 */
public class VenueSearchConstants {


    /**
     * 分页默认值
     */
    public static final Integer DEFAULT_PAGE_NUM = 0;
    public static final Integer DEFAULT_PAGE_SIZE = 10;

    /**
     * 价格范围默认值（固定返回0-500）
     */
    public static final BigDecimal DEFAULT_PRICE_MIN = BigDecimal.ZERO;
    public static final BigDecimal DEFAULT_PRICE_MAX = new BigDecimal("500");
}

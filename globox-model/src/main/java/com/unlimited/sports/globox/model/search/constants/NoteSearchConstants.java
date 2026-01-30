package com.unlimited.sports.globox.model.search.constants;

/**
 * 笔记搜索常量
 */
public class NoteSearchConstants {

    /**
     * 热度分数中，点赞的权重
     */
    public static final Double LIKE_WEIGHT = 1.0;

    /**
     * 热度分数中，评论的权重
     */
    public static final Double COMMENT_WEIGHT = 0.5;

    /**
     * 热度分数中，收藏的权重
     */
    public static final Double COLLECT_WEIGHT = 2.0;

    /**
     * 时间衰减因子 - 每天衰减比例（0-1之间）
     * 0.95 表示每天衰减5%
     */
    public static final Double TIME_DECAY_FACTOR = 0.95;

    /**
     * 时间衰减单位 - 以天为单位
     */
    public static final Integer TIME_DECAY_UNIT_HOURS = 24;

    /**
     * 笔记标题搜索权重
     */
    public static final Float TITLE_SEARCH_WEIGHT = 2.0f;

    /**
     * 笔记内容搜索权重
     */
    public static final Float CONTENT_SEARCH_WEIGHT = 1.0f;

    /**
     * 笔记初始质量分数  todo （后续可由后台更新）
     */
    public static final Double INITIAL_QUALITY_SCORE = 1.0;
}

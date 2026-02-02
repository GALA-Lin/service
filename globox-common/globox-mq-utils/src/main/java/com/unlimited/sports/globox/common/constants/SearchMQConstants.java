package com.unlimited.sports.globox.common.constants;

/**
 * 搜索服务消息队列常量类
 */
public class SearchMQConstants {

    /**
     * 搜索服务统一Topic Exchange
     */
    public static final String EXCHANGE_TOPIC_SEARCH = "exchange.topic.search";

    /**
     * 笔记同步队列：笔记发布/修改/删除同步到ES
     */
    public static final String ROUTING_NOTE_SYNC = "search.note.sync";
    public static final String QUEUE_NOTE_SYNC = "queue.search.note.sync";

    /**
     * 笔记同步操作类型
     */
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
}

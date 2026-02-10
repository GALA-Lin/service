package com.unlimited.sports.globox.model.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 搜索数据同步记录表
 * 每次定时同步任务执行后写入一条记录，用于追踪同步进度和排查问题。
 * 下次同步时查询最近一条成功记录的 syncTime 作为增量起点。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("search_sync_record")
public class SearchSyncRecord {

    @TableId(value = "sync_id", type = IdType.AUTO)
    private Long syncId;

    /**
     * 同步数据类型，对应 SearchDocTypeEnum 的 value
     * 如: VENUE / NOTE / USER / COACH / RALLY
     */
    private String dataType;

    /**
     * 本次同步开始时间
     * 下次同步时以此作为 updatedTime 参数的起点
     */
    private LocalDateTime syncTime;

    /**
     * 本次同步条数
     */
    private Integer syncCount;

    /**
     * 同步状态: 0=成功, 1=失败
     */
    private Integer syncStatus;

    /**
     * 同步结果信息（失败时记录异常信息）
     */
    private String syncMessage;

    private LocalDateTime createdAt;

    /** 同步状态常量 */
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_FAILED = 1;
}

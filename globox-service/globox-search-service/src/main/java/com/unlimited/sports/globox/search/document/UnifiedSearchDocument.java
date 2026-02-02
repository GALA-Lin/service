package com.unlimited.sports.globox.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.geo.Point;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一搜索文档 - 轻量级ES索引
 * 用于全局搜索（如搜"网球"返回场馆+教练+笔记+约球混合结果）
 * 
 * 设计原则：
 * - 只保留搜索必需的公共字段
 * - 各类型专属的复杂筛选放到独立索引（VenueSearchDocument、NoteSearchDocument等）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "#{@indexNameProvider.unifiedSearchIndex()}")
public class UnifiedSearchDocument {

    /**
     * ES文档ID: {type}_{businessId}，如 VENUE_123, NOTE_456
     */
    @Id
    private String id;

    /**
     * 业务实体ID
     */
    @Field(type = FieldType.Long)
    private Long businessId;

    /**
     * 数据类型: VENUE | COACH | NOTE | RALLY | USER
     */
    @Field(type = FieldType.Keyword)
    private String dataType;

    /**
     * 搜索标题 - 场馆名/教练昵称/笔记标题/约球标题/用户昵称
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /**
     * 详细内容 - 场馆描述/教练简介/笔记正文/约球宣言
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /**
     * 通用标签 设施/专长/标签等
     */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    /**
     * 地理位置（场馆/教练/约球）
     */
    @GeoPointField
    private Point location;

    /**
     * 行政区域
     */
    @Field(type = FieldType.Keyword)
    private String region;

    /**
     * 封面图URL
     */
    @Field(type = FieldType.Keyword, index = false)
    private String coverUrl;

    /**
     * 综合评分/热度分数（用于排序）
     */
    @Field(type = FieldType.Double)
    private Double score;

    /**
     * 业务状态
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss || strict_date_optional_time || epoch_millis")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss || strict_date_optional_time || epoch_millis")
    private LocalDateTime updatedAt;
}

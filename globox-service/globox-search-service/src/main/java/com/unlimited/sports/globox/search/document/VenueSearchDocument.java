package com.unlimited.sports.globox.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.geo.Point;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场馆搜索文档 - 场馆独立ES索引
 * 用于场馆列表页的复杂筛选查询
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "#{@indexNameProvider.venueIndex()}")
public class VenueSearchDocument {

    /**
     * ES文档ID: venue_{venueId}
     */
    @Id
    private String id;

    /**
     * 场馆业务ID
     */
    @Field(type = FieldType.Long)
    private Long venueId;

    /**
     * 场馆名称
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    /**
     * 场馆名称 - 单字符分词（用于单字搜索）
     * 注：此字段仅用于 ES 查询，不需要反序列化到 Java 对象
     */
    @Transient
    @Field(name = "name.char", type = FieldType.Text, analyzer = "standard")
    private String nameChar;

    /**
     * 场馆描述
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /**
     * 行政区域
     */
    @Field(type = FieldType.Keyword)
    private String region;

    /**
     * 地理位置
     */
    @GeoPointField
    private Point location;

    /**
     * 封面图URL
     */
    @Field(type = FieldType.Keyword, index = false)
    private String coverUrl;

    /**
     * 平均评分
     */
    @Field(type = FieldType.Double)
    private Double rating;

    /**
     * 评分数量
     */
    @Field(type = FieldType.Integer)
    private Integer ratingCount;

    /**
     * 最低价格
     */
    @Field(type = FieldType.Double)
    private Double priceMin;

    /**
     * 最高价格
     */
    @Field(type = FieldType.Double)
    private Double priceMax;

    /**
     * 场馆类型: 1=HOME(自有) / 2=AWAY(第三方)
     */
    @Field(type = FieldType.Integer)
    private Integer venueType;

    /**
     * 球场类型code列表
     */
    @Field(type = FieldType.Integer)
    private List<Integer> courtTypes;

    /**
     * 地面类型code列表
     */
    @Field(type = FieldType.Integer)
    private List<Integer> groundTypes;

    /**
     * 场馆设施列表（code）
     */
    @Field(type = FieldType.Integer)
    private List<Integer> facilities;

    /**
     * 球场数量
     */
    @Field(type = FieldType.Integer)
    private Integer courtCount;

    /**
     * 业务状态: 1=启用
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

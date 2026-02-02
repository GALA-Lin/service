package com.unlimited.sports.globox.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记搜索文档 - 笔记独立ES索引
 * 用于笔记列表页的搜索和筛选
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "#{@indexNameProvider.noteIndex()}")
public class NoteSearchDocument {

    /**
     * ES文档ID: note_{noteId}
     */
    @Id
    private String id;

    /**
     * 笔记业务ID
     */
    @Field(type = FieldType.Long)
    private Long noteId;

    /**
     * 笔记标题
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /**
     * 笔记内容
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /**
     * 标签列表
     */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    /**
     * 作者用户ID
     */
    @Field(type = FieldType.Long)
    private Long userId;

    /**
     * 作者昵称（用于搜索，不用于展示）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String userName;

    /**
     * 封面图URL
     */
    @Field(type = FieldType.Keyword, index = false)
    private String coverUrl;

    /**
     * 图片列表URLs
     */
    @Field(type = FieldType.Keyword, index = false)
    private List<String> imageUrls;

    /**
     * 点赞数
     */
    @Field(type = FieldType.Integer)
    private Integer likes;

    /**
     * 评论数
     */
    @Field(type = FieldType.Integer)
    private Integer comments;

    /**
     * 收藏数
     */
    @Field(type = FieldType.Integer)
    private Integer saves;

    /**
     * 媒体类型: IMAGE / VIDEO
     */
    @Field(type = FieldType.Keyword)
    private String mediaType;

    /**
     * 是否允许评论
     */
    @Field(type = FieldType.Boolean)
    private Boolean allowComment;

    /**
     * 热度分数（用于热度排序）
     */
    @Field(type = FieldType.Double)
    private Double hotScore;

    /**
     * 质量分数（用于精选排序）
     */
    @Field(type = FieldType.Double)
    private Double qualityScore;

    /**
     * 是否精选
     */
    @Field(type = FieldType.Boolean)
    private Boolean featured;

    /**
     * 笔记状态
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

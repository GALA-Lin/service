package com.unlimited.sports.globox.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

/**
 * 用户搜索文档 - 用户独立ES索引
 * 用于用户搜索（通过昵称、球盒号搜索）
 * 只保留搜索和展示必要的字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "#{@indexNameProvider.userIndex()}")
public class UserSearchDocument {

    /**
     * ES文档ID: user_{userId}
     */
    @Id
    private String id;

    /**
     * 用户ID
     */
    @Field(type = FieldType.Long)
    private Long userId;

    /**
     * 用户昵称（用于搜索和展示）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String nickName;

    /**
     * 球盒号（保留原始大小写，用于展示）
     */
    @Field(type = FieldType.Keyword)
    private String username;

    /**
     * 球盒号小写（用于搜索匹配）
     */
    @Field(type = FieldType.Keyword)
    private String usernameLower;

    /**
     * 头像URL
     */
    @Field(type = FieldType.Keyword, index = false)
    private String avatarUrl;

    /**
     * 性别: 0=女, 1=男
     */
    @Field(type = FieldType.Integer)
    private Integer gender;

    /**
     * 网球水平 NTRP
     */
    @Field(type = FieldType.Double)
    private BigDecimal ntrp;
}

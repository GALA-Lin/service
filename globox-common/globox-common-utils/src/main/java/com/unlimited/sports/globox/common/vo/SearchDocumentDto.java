package com.unlimited.sports.globox.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一搜索文档映射DTO
 * 从UnifiedSearchDocument提取的必要字段，用于转换为各个type的VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchDocumentDto {

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 搜索标题
     */
    private String title;

    /**
     * 详细内容
     */
    private String content;

    /**
     * 通用标签
     */
    private List<String> tags;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 行政区/城市
     */
    private String region;

    /**
     * 创建者/作者ID
     */
    private String creatorId;

    /**
     * 创建者昵称
     */
    private String creatorName;

    /**
     * 创建者头像URL
     */
    private String creatorAvatar;

    /**
     * 封面图/缩略图URL
     */
    private String coverUrl;

    /**
     * 图片列表URLs
     */
    private List<String> imageUrls;

    /**
     * 平均评分
     */
    private Double rating;

    /**
     * 评分数量
     */
    private Integer ratingCount;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 距离（公里），由ES的geo_distance查询计算
     */
    private BigDecimal distance;

    // ==================== 场馆专属字段 ====================

    /**
     * 场馆类型
     */
    private String venueType;

    /**
     * 球场类型code列表
     */
    private List<Integer> venueCourtTypes;

    /**
     * 地面类型code列表
     */
    private List<Integer> venueGroundTypes;

    /**
     * 场馆设施列表（code）
     */
    private List<Integer> venueFacilities;

    /**
     * 球场数量
     */
    private Integer venueCourtCount;

    /**
     * 最低价格
     */
    private Double priceMin;

    // ==================== 教练专属字段 ====================

    /**
     * 教练电话
     */
    private String coachPhone;

    /**
     * 教练常驻服务区域
     */
    private String coachServiceArea;

    /**
     * 常驻区域最低授课时长（小时）
     */
    private Integer coachMinHours;

    /**
     * 可接受的远距离服务区域
     */
    private String coachRemoteServiceArea;

    /**
     * 远距离区域最低授课时长（小时）
     */
    private Integer coachRemoteMinHours;

    /**
     * 教龄（年）
     */
    private Integer coachTeachingYears;

    /**
     * 认证等级列表
     */
    private List<String> coachCertificationLevels;

    /**
     * 是否推荐教练
     */
    private Boolean coachIsRecommended;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 最低技术水平
     */
    private Double ntrpMin;
}

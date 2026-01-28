package com.unlimited.sports.globox.model.venue.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 场馆数据同步VO
 * 用于从venue-service同步场馆数据到search-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueSyncVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 场馆描述
     */
    private String venueDescription;

    /**
     * 行政区 / 城市
     */
    private String region;

    /**
     * 最低价格
     */
    private BigDecimal priceMin;

    /**
     * 最高价格
     */
    private BigDecimal priceMax;

    /**
     * 平均评分 (0-5)
     */
    private BigDecimal rating;

    /**
     * 评分数量
     */
    private Integer ratingCount;

    /**
     * 封面图URL
     */
    private String coverUrl;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 场馆类型: 1=HOME(自有) / 2=AWAY(第三方)
     */
    private Integer venueType;

    /**
     * 球场数量
     */
    private Integer courtCount;

    /**
     * 球场类型code列表
     */
    private List<Integer> courtTypes;

    /**
     * 地面类型code列表
     */
    private List<Integer> groundTypes;

    /**
     * 场馆设施列表（code）
     */
    private List<Integer> facilities;

    /**
     * 业务状态: 1=ACTIVE / 0=INACTIVE
     */
    private Integer status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}

package com.unlimited.sports.globox.model.coach.vo;

import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * @since 2025/12/31 13:59
 * 供用户查询的教练信息列表项
 */
@Data
@Builder
public class CoachItemVo {

    /**
     * 教练用户信息，包含用户基本信息：头像、昵称、性别等
     */
    private UserInfoVo coachUserInfo;

    /**
     * 教练电话
     */
    private String coachPhone;

    /**
     * 常驻区域
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
     * 教龄
     */
    private Integer coachTeachingYears;

    /**
     * 评分
     */
    private BigDecimal coachRatingScore;

    /**
     * 评论数
     */
    private Integer coachRatingCount;

    /**
     * 教练价格下限，XX元起
     */
    private BigDecimal coachMinPrice;

    /**
     * 教练证书标签列表（字符串形式）
     */
    private List<String> coachCertificationLevels;

    /**
     * 距离（公里），当提供用户位置时返回
     */
    private BigDecimal distance;

    /**
     * 是否推荐教练
     */
    private Boolean isRecommended;
}

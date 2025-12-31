package com.unlimited.sports.globox.model.coach.vo;

import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * @since 2025/12/31 13:59
 * 教练信息列表项
 */
@Data
@Builder
public class CoachListItemVo {

    /**
     * 教练用户信息，包含用户基本信息：头像、昵称、性别等
     */
    private UserInfoVo coachUserInfo;

    /**
     * 常驻区域
     */
    private String coachRemoteServiceArea;

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
     * 教练证书数组
     */
    private List<Integer> coachCertificationLevels;


}

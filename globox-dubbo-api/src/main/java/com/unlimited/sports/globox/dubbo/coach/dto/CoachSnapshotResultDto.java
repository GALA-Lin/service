package com.unlimited.sports.globox.dubbo.coach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 教练快照结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachSnapshotResultDto implements Serializable {

    /**
     * 教练用户ID
     */
    @NotNull
    private Long coachUserId;

    /**
     * 教练姓名
     */
    private String coachName;

    /**
     * 教练头像
     */
    private String coachAvatar;

    /**
     * 教练联系电话
     */
    private String coachPhone;

    /**
     * 常驻服务区域
     */
    private String serviceArea;

    /**
     * 证书等级列表
     */
    private List<String> certificationLevels;

    /**
     * 教学年限
     */
    private Integer teachingYears;

    /**
     * 专长标签
     */
    private List<String> specialtyTags;

    /**
     * 综合评分
     */
    private BigDecimal ratingScore;

    /**
     * 评价数
     */
    private Integer ratingCount;

    /**
     * 时段快照列表
     */
    @NotNull
    private List<CoachSlotSnapshotDto> slotSnapshots;
}
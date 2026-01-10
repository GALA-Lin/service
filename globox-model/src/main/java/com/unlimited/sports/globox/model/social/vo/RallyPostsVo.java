package com.unlimited.sports.globox.model.social.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.unlimited.sports.globox.model.social.dto.RallyPostsDto;
import com.unlimited.sports.globox.model.social.dto.RallyQueryDto;
import com.unlimited.sports.globox.model.social.entity.RallyActivityTypeEnum;
import com.unlimited.sports.globox.model.social.entity.RallyParticipant;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 约球帖子视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RallyPostsVo {

    /**
     * 约球帖子ID
     */
    @NonNull
    private Long rallyPostId;

    /**
     * 约球发起人ID
     */
    @NonNull
    private Long rallyInitiatorId;

    /**
     * 用户头像URL
     */
    private String avatarUrl;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 约球标题
     */
    private String rallyTitle;

    /**
     * 约球地区
     */
    private String rallyRegion;

    /**
     * 约球场馆名称
     */
    private String rallyVenueName;

    /**
     * 约球场地名称
     */
    private String rallyCourtName;

    /**
     * 约球活动日期
     */
    @NonNull
    private LocalDate rallyEventDate;

    /**
     * 约球开始时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private LocalTime rallyStartTime;

    /**
     * 约球结束时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private LocalTime rallyEndTime;

    /**
     * 约球标签
     */
    private List<String> rallyLabel;

    /**
     * NTRP最低等级
     */
    private double ntrpMin;

    /**
     * NTRP最高等级
     */
    private double ntrpMax;


    /**
     * 约球总人数
     */
    private Integer rallyTotalPeople;

    /**
     * 约球状态
     */
    private String rallyStatus;

    /**
     * 约球参与人列表
     */
    @NonNull
    private List<RallyParticipantVo> rallyParticipants;

    /**
     * 约球创建时间
     */
    private LocalDateTime createdAt;

    private Integer rallyRemainingPeople;

}

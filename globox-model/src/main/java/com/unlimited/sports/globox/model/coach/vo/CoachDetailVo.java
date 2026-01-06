package com.unlimited.sports.globox.model.coach.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @since 2025/12/31 14:24
 * 教练详情
 */
@Data
@Builder
public class CoachDetailVo {

    /**
     * 列表中所有信息：名字、头像、昵称、性别、常驻区域、教龄、评分以及评论数、教练资质/证书、最低课程价格
     */
    private CoachItemVo coachSimpleInfo;

    /**
     * 授课总数:累计完成课程数（订单数）
     */
    private Integer coachTotalCourses;

    /**
     * 累计学员数
     */
    private Integer coachTotalStudents;

    /**
     * 累计授课时长
     */
    private java.math.BigDecimal coachTotalHours;

    /**
     * 教学介绍/教学风格
     */
    private String coachTeachingStyle;

    /**
     * 教学图片URL列表
     */
    private List<String> coachWorkPhotos;

    /**
     * 教学视频URL列表
     */
    private List<VideoItem> coachWorkVideos;

    /**
     * 专长标签列表
     */
    private List<String> coachSpecialtyTags;

    /**
     * 可接受的远距离服务区域
     */
    private String coachRemoteServiceArea;

    /**
     * 远距离区域最低授课时长（小时）
     */
    private Integer coachRemoteMinHours;

    /**
     * 接受场地类型
     */
    private Integer coachAcceptVenueType;

    /**
     * 接受场地类型描述
     */
    private String coachAcceptVenueTypeDesc;

    /**
     * 课程类型列表（包含价格信息）
     */
    private List<CoachServiceVo> services;

    /**
     * 证书附件URL列表
     */
    private List<String> coachCertificationFiles;
}
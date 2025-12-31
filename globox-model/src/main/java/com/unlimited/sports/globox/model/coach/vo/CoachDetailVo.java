package com.unlimited.sports.globox.model.coach.vo;

import lombok.Builder;
import lombok.Data;

/**
 * @since 2025/12/31 14:24
 * 教练详情
 */
@Data
@Builder
public class CoachDetailVo {

    /**
     * 列表中所有信息：名字、头像、常驻区域、教龄、评分以及评论数、教练资质/证书、最低课程价格
     */
    private CoachListItemVo  coachSimpleInfo;

    /**
     * 授课总数:累计完成课程数（订单数）
     */
    private Integer coachTotalCourses;

    /**
     * 累计学员数
     */
    private Integer coachTotalStudents;

    /**
     * 教学介绍
     */
    private String coachTeachingStyle;


}

package com.unlimited.sports.globox.model.coach.dto;

import com.unlimited.sports.globox.model.coach.vo.VideoItem;
import lombok.Data;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * @since 2026/1/12
 * 更新教练展示信息DTO
 */
@Data
public class UpdateCoachDisplaySettingsDto {

    /**
     * 教练用户ID（从请求头获取）
     */
    private Long coachUserId;

    /**
     * 教学图片URL列表
     */
    @Size(max = 9, message = "教学图片最多9张")
    private List<String> coachWorkPhotos;

    /**
     * 教学视频列表
     */
    @Size(max = 5, message = "教学视频最多5个")
    private List<VideoItem> coachWorkVideos;

    /**
     * 证书附件URL列表
     */
    @Size(max = 10, message = "证书附件最多10个")
    private List<String> coachCertificationFiles;

    /**
     * 证书等级列表
     */
    @Size(max = 10, message = "证书等级最多10个")
    private List<String> coachCertificationLevel;
}
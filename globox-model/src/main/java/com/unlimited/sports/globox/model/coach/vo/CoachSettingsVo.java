package com.unlimited.sports.globox.model.coach.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @since 2026/1/12
 * 教练设置信息VO（优化版）
 *
 * 优化说明：
 * 1. ServiceAreaInfo 中移除了 coachServiceArea 和 coachRemoteServiceArea 字符串字段
 * 2. 只保留 serviceAreaList 和 remoteServiceAreaList 数组字段
 * 3. 前端直接使用数组，无需额外解析
 */
@Data
@Builder
public class CoachSettingsVo {

    /**
     * 教练用户ID
     */
    private Long coachUserId;

    /**
     * 教练状态：0-暂停接单，1-正常接单，2-休假中
     */
    private Integer coachStatus;

    /**
     * 教练状态描述
     */
    private String coachStatusDesc;

    /**
     * 教练位置信息
     */
    private LocationInfo locationInfo;

    /**
     * 服务区域信息
     */
    private ServiceAreaInfo serviceAreaInfo;

    /**
     * 基本设置信息
     */
    private BasicSettingsInfo basicSettingsInfo;

    /**
     * 展示设置信息
     */
    private DisplaySettingsInfo displaySettingsInfo;

    /**
     * 场地偏好设置
     */
    private VenuePreferenceInfo venuePreferenceInfo;

    /**
     * 真名显示信息
     */
    private RealNameInfo realNameInfo;

    /**
     * 真名显示信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RealNameInfo {
        /**
         * 真实姓名
         */
        private String coachRealName;

        /**
         * 是否显示真名
         */
        private Boolean displayRealName;
    }

    /**
     * 位置信息
     */
    @Data
    @Builder
    public static class LocationInfo {
        /**
         * 纬度
         */
        private Double latitude;

        /**
         * 经度
         */
        private Double longitude;
    }

    /**
     * 服务区域信息（优化版）
     */
    @Data
    @Builder
    public static class ServiceAreaInfo {
        /**
         * 常驻服务区域列表
         * 示例：["双流区","金牛区"]
         */
        private String serviceArea;

        /**
         * 远距离服务区域列表
         * 示例：["锦江区","成华区"]
         */
        private List<String> remoteServiceAreaList;

        /**
         * 远距离最低授课时长（小时）
         */
        private Integer coachRemoteMinHours;
    }

    /**
     * 基本设置信息
     */
    @Data
    @Builder
    public static class BasicSettingsInfo {
        /**
         * 教学风格
         */
        private String coachTeachingStyle;

        /**
         * 专长标签
         */
        private List<String> coachSpecialtyTags;

        /**
         * 主要奖项
         */
        private List<String> coachAward;

        /**
         * 教龄
         */
        private Integer coachTeachingYears;
    }

    /**
     * 展示设置信息
     */
    @Data
    @Builder
    public static class DisplaySettingsInfo {
        /**
         * 教学图片
         */
        private List<String> coachWorkPhotos;

        /**
         * 教学视频
         */
        private List<String> coachWorkVideos;

        /**
         * 证书附件
         */
        private List<String> coachCertificationFiles;

        /**
         * 证书等级
         */
        private List<String> coachCertificationLevel;
    }

    /**
     * 场地偏好信息
     */
    @Data
    @Builder
    public static class VenuePreferenceInfo {
        /**
         * 接受场地类型
         */
        private Integer coachAcceptVenueType;

        /**
         * 接受场地类型描述
         */
        private String coachAcceptVenueTypeDesc;
    }
}
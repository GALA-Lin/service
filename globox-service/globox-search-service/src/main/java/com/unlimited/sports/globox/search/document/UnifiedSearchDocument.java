package com.unlimited.sports.globox.search.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.geo.Point;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 统一搜索文档 - Elasticsearch索引结构
 *
 * 字段分层：
 * 1. 公共字段 - 所有type都有
 * 2. 通用过滤字段 - 跨type的语义一致维度
 * 3. Type专属字段 - 各自特定的字段（前缀化）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "globox-unified-search")
public class UnifiedSearchDocument {

    // ==================== 公共字段 ====================

    /**
     * ES文档自增ID（与业务无关）
     */
    @Id
    private String id;

    /**
     * 业务实体ID（纯数字）
     * 存储场馆ID、教练ID、笔记ID、约球ID、用户ID等
     */
    @Field(type = FieldType.Long)
    private Long businessId;

    /**
     * 数据类型: VENUE | COACH | NOTE | RALLY | USER
     */
    @Field(type = FieldType.Keyword)
    private String dataType;

    /**
     * 搜索标题 - 场馆名/教练昵称/笔记标题/约球标题/用户昵称
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /**
     * 详细内容 - 场馆描述/教练简介/笔记正文/约球宣言/用户签名
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /**
     * 通用标签 - 设施/专长/标签等
     */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    /**
     * 地理位置 - 支持距离查询和范围查询
     * 场馆/教练有值，笔记/约球/用户为null
     */
    @GeoPointField
    private Point location;

    /**
     * 行政区 / 城市 - 区域级别的过滤
     */
    @Field(type = FieldType.Keyword)
    private String region;

    /**
     * 创建者/作者ID - 教练/笔记/约球/用户的ID
     */
    @Field(type = FieldType.Keyword)
    private String creatorId;

    /**
     * 创建者昵称 - 用于展示
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String creatorName;

    /**
     * 创建者头像URL
     */
    @Field(type = FieldType.Keyword)
    private String creatorAvatar;

    /**
     * 封面图/缩略图URL - 笔记/约球的首图
     */
    @Field(type = FieldType.Keyword)
    private String coverUrl;

    /**
     * 图片列表URLs - 完整的图片集合
     */
    @Field(type = FieldType.Keyword)
    private List<String> imageUrls;

    /**
     * 平均评分 (0-5) - 场馆/教练有值
     */
    @Field(type = FieldType.Double)
    private Double rating;

    /**
     * 评分数量 - 场馆/教练有值
     */
    @Field(type = FieldType.Integer)
    private Integer ratingCount;

    /**
     * 点赞数 - 笔记有值
     */
    @Field(type = FieldType.Integer)
    private Integer likes;

    /**
     * 评论数 - 笔记有值
     */
    @Field(type = FieldType.Integer)
    private Integer comments;

    /**
     * 收藏/保存数 - 笔记有值
     */
    @Field(type = FieldType.Integer)
    private Integer saves;

    /**
     * 业务状态 - 1=ACTIVE, 2=INACTIVE等
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss || strict_date_optional_time || epoch_millis")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss || strict_date_optional_time || epoch_millis")
    private LocalDateTime updatedAt;
    /**
     * 热度分数 - 用于热度排序
     */
    @Field(type = FieldType.Double)
    private Double hotScore;

    /**
     * 质量分数 - 用于质量排序
     */
    @Field(type = FieldType.Double)
    private Double qualityScore;

    /**
     * 人工权重 - 后台可调的权重值
     */
    @Field(type = FieldType.Double)
    private Double boost;

    /**
     * 权重值 - 用于混合排序
     */
    @Field(type = FieldType.Integer)
    private Integer weight;

    // ==================== 通用过滤字段 ====================

    /**
     * 性别 - 教练/用户: 0=女, 1=男
     */
    @Field(type = FieldType.Integer)
    private Integer gender;

    /**
     * 最低技术水平 - 教练/约球/用户的NTRP等级
     */
    @Field(type = FieldType.Double)
    private Double ntrpMin;

    /**
     * 最高技术水平 - 教练/约球/用户的NTRP等级
     */
    @Field(type = FieldType.Double)
    private Double ntrpMax;

    /**
     * 最低价格 - 场馆/教练的价格范围下限
     */
    @Field(type = FieldType.Double)
    private Double priceMin;

    /**
     * 最高价格 - 教练的价格范围上限
     */
    @Field(type = FieldType.Double)
    private Double priceMax;

    // ==================== Type专属字段 - 场馆(venue) ====================

    /**
     * 场馆类型: 1=HOME(自有) / 2=AWAY(第三方)
     */
    @Field(type = FieldType.Integer)
    private Integer venueType;

    /**
     * 球场类型code列表
     */
    @Field(type = FieldType.Keyword)
    private List<Integer> venueCourtTypes;

    /**
     * 地面类型code列表
     */
    @Field(type = FieldType.Keyword)
    private List<Integer> venueGroundTypes;

    /**
     * 场馆设施列表（code）
     */
    @Field(type = FieldType.Keyword)
    private List<Integer> venueFacilities;

    /**
     * 球场数量
     */
    @Field(type = FieldType.Integer)
    private Integer venueCourtCount;

    // ==================== Type专属字段 - 教练(coach) ====================

    /**
     * 教练电话
     */
    @Field(type = FieldType.Keyword)
    private String coachPhone;

    /**
     * 教练常驻服务区域
     */
    @Field(type = FieldType.Keyword)
    private String coachServiceArea;

    /**
     * 常驻区域最低授课时长（小时）
     */
    @Field(type = FieldType.Integer)
    private Integer coachMinHours;

    /**
     * 可接受的远距离服务区域
     */
    @Field(type = FieldType.Keyword)
    private String coachRemoteServiceArea;

    /**
     * 远距离区域最低授课时长（小时）
     */
    @Field(type = FieldType.Integer)
    private Integer coachRemoteMinHours;

    /**
     * 教龄（年）
     */
    @Field(type = FieldType.Integer)
    private Integer coachTeachingYears;

    /**
     * 认证等级列表
     */
    @Field(type = FieldType.Keyword)
    private List<String> coachCertificationLevels;

    /**
     * 奖项/成就列表
     */
    @Field(type = FieldType.Keyword)
    private List<String> coachAwards;

    /**
     * 教学专长标签
     */
    @Field(type = FieldType.Keyword)
    private List<String> coachSpecialtyTags;

    /**
     * 教学风格描述
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String coachTeachingStyle;

    /**
     * 累计学员数
     */
    @Field(type = FieldType.Integer)
    private Integer coachTotalStudents;

    /**
     * 累计教学时数
     */
    @Field(type = FieldType.Double)
    private Double coachTotalHours;

    /**
     * 累计课程数
     */
    @Field(type = FieldType.Integer)
    private Integer coachTotalCourses;

    /**
     * 教练账户状态: 0=暂停接单, 1=正常接单, 2=休假中
     */
    @Field(type = FieldType.Integer)
    private Integer coachStatus;

    /**
     * 是否推荐教练
     */
    @Field(type = FieldType.Boolean)
    private Boolean coachIsRecommended;

    /**
     * 教练显示排序权重
     */
    @Field(type = FieldType.Integer)
    private Integer coachDisplayOrder;

    // ==================== Type专属字段 - 笔记(note) ====================

    /**
     * 媒体类型: IMAGE / VIDEO
     */
    @Field(type = FieldType.Keyword)
    private String noteMediaType;

    /**
     * 是否允许评论
     */
    @Field(type = FieldType.Boolean)
    private Boolean noteAllowComment;

    // ==================== Type专属字段 - 约球(rally) ====================

    /**
     * 约球场馆名称
     */
    @Field(type = FieldType.Keyword)
    private String rallyVenueName;

    /**
     * 约球球场名称
     */
    @Field(type = FieldType.Keyword)
    private String rallyCourtName;

    /**
     * 活动日期
     */
    @Field(type = FieldType.Date)
    private LocalDate rallyEventDate;

    /**
     * 活动开始时间
     */
    @Field(type = FieldType.Keyword)
    private String rallyStartTime;

    /**
     * 活动结束时间
     */
    @Field(type = FieldType.Keyword)
    private String rallyEndTime;

    /**
     * 活动时段: 上午/下午/晚上
     */
    @Field(type = FieldType.Keyword)
    private String rallyTimeType;

    /**
     * 活动总人数
     */
    @Field(type = FieldType.Integer)
    private Integer rallyTotalPeople;

    /**
     * 当前参加人数
     */
    @Field(type = FieldType.Integer)
    private Integer rallyCurrentPeople;

    /**
     * 剩余人数
     */
    @Field(type = FieldType.Integer)
    private Integer rallyRemainingPeople;

    /**
     * 费用/价格
     */
    @Field(type = FieldType.Double)
    private Double rallyCost;

    /**
     * 费用承担方式: 0=发起人承担, 1=AA分摊
     */
    @Field(type = FieldType.Integer)
    private Integer rallyCostBearer;

    /**
     * 活动类型: 0=不限, 1=单打, 2=双打
     */
    @Field(type = FieldType.Keyword)
    private String rallyActivityType;

    /**
     * 性别限制: 0=不限, 1=仅男生, 2=仅女生
     */
    @Field(type = FieldType.Integer)
    private Integer rallyGenderLimit;

    /**
     * 备注信息
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String rallyNotes;

    // ==================== Type专属字段 - 用户(user) ====================

    /**
     * 用户球星卡肖像URL
     */
    @Field(type = FieldType.Keyword)
    private String userPortraitUrl;

    /**
     * 开始打网球的年份
     */
    @Field(type = FieldType.Integer)
    private Integer userSportsStartYear;

    /**
     * 持拍手: LEFT / RIGHT
     */
    @Field(type = FieldType.Keyword)
    private String userPreferredHand;

    /**
     * 常驻区域
     */
    @Field(type = FieldType.Keyword)
    private String userHomeDistrict;

    /**
     * 技能维度-力量 (0-10)
     */
    @Field(type = FieldType.Integer)
    private Integer userPower;

    /**
     * 技能维度-速度 (0-10)
     */
    @Field(type = FieldType.Integer)
    private Integer userSpeed;

    /**
     * 技能维度-发球 (0-10)
     */
    @Field(type = FieldType.Integer)
    private Integer userServe;

    /**
     * 技能维度-截击 (0-10)
     */
    @Field(type = FieldType.Integer)
    private Integer userVolley;

    /**
     * 技能维度-耐力 (0-10)
     */
    @Field(type = FieldType.Integer)
    private Integer userStamina;

    /**
     * 技能维度-心理素质 (0-10)
     */
    @Field(type = FieldType.Integer)
    private Integer userMental;

    /**
     * 用户账户状态: ACTIVE / DISABLED
     */
    @Field(type = FieldType.Keyword)
    private String userStatus;

    /**
     * 用户角色: USER / COACH / ADMIN
     */
    @Field(type = FieldType.Keyword)
    private String userRole;
}

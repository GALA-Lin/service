package com.unlimited.sports.globox.model.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * @since 2025-12-18-10:41
 * 场馆信息表
 */

@Data
@Slf4j
@TableName("venues")
public class Venue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 场馆ID，主键
     */
    @TableId(value = "venue_id", type = IdType.AUTO)
    private Long venueId;

    /**
     * 所属商家ID
     */
    @TableField("merchant_id")
    private Long merchantId;

    /**
     * 场馆官方名称
     */
    @TableField("name")
    private String name;

    /**
     * 场馆详细地址
     */
    @TableField("address")
    private String address;

    /**
     * 所属区域或行政区
     */
    @TableField("region")
    private String region;

    /**
     * 纬度，用于距离计算
     */
    @TableField("latitude")
    private BigDecimal latitude;

    /**
     * 经度，用于距离计算
     */
    @TableField("longitude")
    private BigDecimal longitude;

    /**
     * 场馆联系电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 场馆文字介绍或简介
     */
    @TableField("description")
    private String description;

    /**
     * 场馆图片URL列表，使用;分隔
     */
    @TableField("image_urls")
    private String imageUrls;

    /**
     * 场馆设施标签列表，使用;分隔
     */
    @TableField("facilities")
    private String facilities;

    /**
     * 场馆平均评分（冗余字段）
     */
    @TableField("avg_rating")
    private BigDecimal avgRating;

    /**
     * 总评分数量（冗余字段）
     */
    @TableField("rating_count")
    private Integer ratingCount;

    /**
     * 球场类型：1=home(自有)，2=away(第三方集成)
     */
    @TableField("venue_type")
    private Integer venueType;

    /**
     * away球场在第三方系统中的ID
     */
    @TableField("third_party_venue_id")
    private String thirdPartyVenueId;


    /**
     * 提前几天开放订场
     */
    @TableField("max_advance_days")
    private Integer maxAdvanceDays;


    /**
     * 几点开放订场
     */
    @TableField("slot_visibility_time")
    private LocalTime slotVisibilityTime;

    /**
     * 绑定的价格模版id
     */
    @TableField("template_id")
    private Long templateId;

    /**
     * 绑定的普通退款规则ID
     */
    @TableField("venue_refund_rule_id")
    private Long venueRefundRuleId;

    /**
     * 绑定的活动退款规则ID
     */
    @TableField("activity_refund_rule_id")
    private Long activityRefundRuleId;



    /**
     * 场地状态: 1 正常 0 暂停营业
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;



    /**
     * 检查是否允许查看该日期的槽位
     * 控制逻辑：
     * 1. 检查当前日期是否在允许的预订范围内（不超过maxAdvanceDays）
     * 2. 检查当前时间是否已经到达slotVisibilityTime
     * @param bookingDate 要查看的预订日期
     */
    public boolean validSlotVisibilityPermission(LocalDate bookingDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        if (maxAdvanceDays == null || maxAdvanceDays < 0) {
            log.warn("[checkSlotVisibilityPermission]场馆{}: {}未配置预定时间",venueId,name);
            return false;
        }

        if (slotVisibilityTime == null) {
            slotVisibilityTime = LocalTime.of(0, 0);
            log.debug("场馆 {} 未配置slotVisibilityTime，使用默认值00:00", venueId);
        }

        // 预订日期不能超过最大提前天数
        long daysUntilBooking = ChronoUnit.DAYS.between(today, bookingDate);
        if (daysUntilBooking < 0) {
            log.warn("用户尝试查看过去的预订日期 - venueId={}, bookingDate={}, 距今{}天", venueId, bookingDate, daysUntilBooking);
            return false;
        }
        if (daysUntilBooking > maxAdvanceDays) {
            log.warn("用户尝试查看过远的预订日期 - venueId={}, bookingDate={}, 距今{}天，最多允许{}天",
                    venueId, bookingDate, daysUntilBooking, maxAdvanceDays);
            return false;
        }

        // 只对“最远可预订日”(today + maxAdvanceDays)做开放时间控制。
        if (daysUntilBooking == maxAdvanceDays && currentTime.isBefore(slotVisibilityTime)) {
            log.warn("用户尝试查看最远可预订日槽位，但还未到开放时间 - venueId={}, bookingDate={}, 当前时间={}, 开放时间={}",
                    venueId, bookingDate, currentTime, slotVisibilityTime);
            return false;
        }
        return true;
    }
}